package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.adapter.out.events.AuctionEventBroker;
import com.carrefour.kata.bidanddrive.auction.adapter.out.events.EventSink;
import com.carrefour.kata.bidanddrive.auction.application.AuctionRepository;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** The event stream over a real server, read like a browser would (spec 009). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuctionEventsApiTests {

	private static final long BIDDER = 7201;
	private static final Duration WAIT = Duration.ofSeconds(2);

	@LocalServerPort
	private int port;

	@Autowired
	private AuctionRepository auctions;

	@Autowired
	private AuctionEventBroker broker;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private JsonMapper json;

	private long auctionId;

	@BeforeEach
	void create_a_bidder_and_an_auction() {
		jdbc.update("INSERT INTO app_user (id, display_name) VALUES (?, 'Bob') ON CONFLICT (id) DO NOTHING", BIDDER);
		auctionId = auctions.save(new Auction(null, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
				new Money(new BigDecimal("1000.00")), new Money(new BigDecimal("100.00")),
				Instant.now().plus(Duration.ofDays(1)), AuctionStatus.OPEN)).id();
	}

	@AfterEach
	void delete_what_the_test_created() {
		jdbc.update("DELETE FROM bid WHERE auction_id = ?", auctionId);
		jdbc.update("DELETE FROM car WHERE auction_id = ?", auctionId);
		jdbc.update("DELETE FROM auction WHERE id = ?", auctionId);
		jdbc.update("DELETE FROM app_user WHERE id = ?", BIDDER);
	}

	@Test
	void a_subscriber_receives_bid_placed_as_an_sse_message_with_the_envelope() {
		try (SseTestClient client = subscribe(Map.of())) {
			ResponseEntity<JsonNode> bid = placeBid("1200.00");

			await().atMost(WAIT).until(() -> client.events("bid-placed").size() == 1);
			JsonNode event = json.readTree(client.events("bid-placed").getFirst());
			assertThat(event.get("type").asString()).isEqualTo("bid-placed");
			assertThat(event.get("auctionId").asLong()).isEqualTo(auctionId);
			assertThat(event.get("occurredAt").asString()).isNotBlank();
			assertThat(event.get("data").get("bidId").asLong()).isEqualTo(bid.getBody().get("id").asLong());
			assertThat(event.get("data").get("amount").asString()).isEqualTo("1200.00");
			assertThat(event.get("data").get("bidderName").asString()).isEqualTo("B***");
			assertThat(event.get("data").get("minNextBid").asString()).isEqualTo("1300.00");
			assertThat(client.lines()).noneMatch(line -> line.startsWith("id:"));
		}
	}

	@Test
	void a_broken_subscriber_does_not_change_the_bid_response() {
		EventSink broken = new BrokenSink();
		broker.subscribe(auctionId, broken);
		try (SseTestClient client = subscribe(Map.of())) {
			ResponseEntity<JsonNode> bid = placeBid("1200.00");

			assertThat(bid.getStatusCode().value()).isEqualTo(201);
			await().atMost(WAIT).until(() -> client.events("bid-placed").size() == 1);
			assertThat(broker.subscriberCount(auctionId)).isEqualTo(1);
		} finally {
			broker.unsubscribe(auctionId, broken);
		}
	}

	@Test
	void an_unknown_auction_is_a_problem_404_and_opens_no_stream() {
		try (SseTestClient client = SseTestClient.open(url(Long.MAX_VALUE), Map.of())) {
			assertThat(client.status()).isEqualTo(404);
			assertThat(client.contentType()).hasValueSatisfying(
					type -> assertThat(type).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
			await().atMost(WAIT).until(() -> String.join("", client.lines()).contains("Auction not found"));
			assertThat(broker.subscriberCount(Long.MAX_VALUE)).isZero();
		}
	}

	@Test
	void a_non_numeric_auction_id_is_a_400() {
		try (SseTestClient client = SseTestClient.open("http://localhost:" + port + "/api/auctions/abc/events",
				Map.of())) {
			assertThat(client.status()).isEqualTo(400);
		}
	}

	@Test
	void the_stream_is_public_without_an_identity() {
		try (SseTestClient client = subscribe(Map.of())) {
			assertThat(client.status()).isEqualTo(200);
			assertThat(client.contentType()).hasValueSatisfying(
					type -> assertThat(type).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE));
		}
	}

	@Test
	void a_malformed_or_unknown_identity_is_treated_as_anonymous() {
		for (String userId : new String[] {"abc", "999999"}) {
			try (SseTestClient client = subscribe(Map.of("X-User-Id", userId))) {
				assertThat(client.status()).isEqualTo(200);
			}
		}
	}

	/** Opens the stream and waits until the server has registered it, so no event can be missed. */
	private SseTestClient subscribe(Map<String, String> headers) {
		int before = broker.subscriberCount(auctionId);
		SseTestClient client = SseTestClient.open(url(auctionId), headers);
		if (client.status() == 200) {
			await().atMost(WAIT).until(() -> broker.subscriberCount(auctionId) > before);
		}
		return client;
	}

	private ResponseEntity<JsonNode> placeBid(String amount) {
		return RestClient.create("http://localhost:" + port).post()
				.uri("/api/auctions/{id}/bids", auctionId)
				.header("X-User-Id", Long.toString(BIDDER))
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("amount", amount))
				.retrieve()
				.toEntity(JsonNode.class);
	}

	private String url(long id) {
		return "http://localhost:" + port + "/api/auctions/" + id + "/events";
	}

	/** Connects, then fails on every event, like a client that went away. */
	private static final class BrokenSink implements EventSink {

		@Override
		public void send(String name, String data) throws IOException {
			throw new IOException("connection reset");
		}

		@Override
		public void comment(String text) {
		}

		@Override
		public void completeWithError(Throwable cause) {
		}

		@Override
		public void onCompletion(Runnable callback) {
		}

		@Override
		public void onTimeout(Runnable callback) {
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
		}

	}

}
