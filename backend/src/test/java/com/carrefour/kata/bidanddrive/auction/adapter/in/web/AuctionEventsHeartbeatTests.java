package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.adapter.out.events.AuctionEventBroker;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.awaitility.Awaitility.await;

/**
 * The heartbeat with a 50 millisecond interval (spec 009). The context is closed afterwards, so its
 * fast scheduler does not keep running while other test classes run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "bidanddrive.events.heartbeat=PT0.05S")
@DirtiesContext
class AuctionEventsHeartbeatTests {

	private static final Duration WAIT = Duration.ofSeconds(2);

	@LocalServerPort
	private int port;

	@Autowired
	private AuctionRepository auctions;

	@Autowired
	private AuctionEventBroker broker;

	@Autowired
	private JdbcTemplate jdbc;

	private long auctionId;

	@BeforeEach
	void create_an_auction() {
		auctionId = auctions.save(new Auction(null, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
				new Money(new BigDecimal("1000.00")), new Money(new BigDecimal("100.00")),
				Instant.now().plus(Duration.ofDays(1)), AuctionStatus.OPEN)).id();
	}

	@AfterEach
	void delete_the_auction() {
		jdbc.update("DELETE FROM car WHERE auction_id = ?", auctionId);
		jdbc.update("DELETE FROM auction WHERE id = ?", auctionId);
	}

	@Test
	void an_idle_subscriber_receives_keepalive_comments() {
		try (SseTestClient client = SseTestClient.open(url(), Map.of())) {
			await().atMost(WAIT).until(() -> client.comments().contains("keepalive"));
		}
	}

	@Test
	void a_client_that_closed_its_connection_is_dropped_at_a_later_write() {
		SseTestClient client = SseTestClient.open(url(), Map.of());
		await().atMost(WAIT).until(() -> broker.subscriberCount(auctionId) == 1);

		client.close();

		await().atMost(WAIT).until(() -> broker.subscriberCount(auctionId) == 0);
	}

	private String url() {
		return "http://localhost:" + port + "/api/auctions/" + auctionId + "/events";
	}

}
