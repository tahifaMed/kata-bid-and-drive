package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parallel bids against a real server and database (ADR 0003). Not transactional: each request commits
 * on its own, so the test creates its own users and auction and deletes them afterwards.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ConcurrentBidsTests {

	private static final long FIRST_BIDDER = 7001;
	private static final int BIDDERS = 20;

	@LocalServerPort
	private int port;

	@Autowired
	private AuctionRepository auctions;

	@Autowired
	private JdbcTemplate jdbc;

	private RestClient client;
	private long auctionId;

	@BeforeEach
	void create_bidders_and_an_auction() {
		client = RestClient.create("http://localhost:" + port);
		LongStream.range(FIRST_BIDDER, FIRST_BIDDER + BIDDERS).forEach(id -> jdbc.update(
				"INSERT INTO app_user (id, display_name) VALUES (?, ?) ON CONFLICT (id) DO NOTHING", id, "Bidder " + id));
		auctionId = auctions.save(new Auction(null, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
				new Money(new BigDecimal("1000.00")), new Money(new BigDecimal("100.00")),
				Instant.now().plus(Duration.ofDays(1)), AuctionStatus.OPEN)).id();
	}

	@AfterEach
	void delete_what_the_test_created() {
		jdbc.update("DELETE FROM bid WHERE auction_id = ?", auctionId);
		jdbc.update("DELETE FROM car WHERE auction_id = ?", auctionId);
		jdbc.update("DELETE FROM auction WHERE id = ?", auctionId);
		jdbc.update("DELETE FROM app_user WHERE id >= ? AND id < ?", FIRST_BIDDER, FIRST_BIDDER + BIDDERS);
	}

	@Test
	void twenty_bidders_posting_the_same_amount_at_once_produce_exactly_one_bid() throws Exception {
		List<Long> bidders = LongStream.range(FIRST_BIDDER, FIRST_BIDDER + BIDDERS).boxed().toList();

		List<Integer> statuses = postAtOnce(bidders);

		assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
		assertThat(statuses).filteredOn(status -> status != 201).allMatch(status -> status == 409 || status == 422);
		assertThat(storedBidCount()).isEqualTo(1);
		assertThat(storedCurrentBid()).isEqualByComparingTo("1000.00");
		assertThat(bidRows()).isEqualTo(1);
	}

	@Test
	void a_double_click_posting_the_same_bid_twice_at_once_produces_one_bid() throws Exception {
		List<Integer> statuses = postAtOnce(List.of(FIRST_BIDDER, FIRST_BIDDER));

		assertThat(statuses).containsExactlyInAnyOrder(201, 409);
		assertThat(storedBidCount()).isEqualTo(1);
		assertThat(bidRows()).isEqualTo(1);
	}

	/** Every request waits on one latch, so they reach the server together. */
	private List<Integer> postAtOnce(List<Long> bidders) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<Integer>> results = bidders.stream()
					.map(bidder -> executor.submit(() -> {
						start.await();
						return post(bidder);
					}))
					.toList();
			start.countDown();
			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> result : results) {
				statuses.add(result.get());
			}
			return statuses;
		}
	}

	private int post(long bidder) {
		return client.post()
				.uri("/api/auctions/{id}/bids", auctionId)
				.header("X-User-Id", String.valueOf(bidder))
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"amount\": \"1000.00\"}")
				.exchange((request, response) -> response.getStatusCode().value());
	}

	private int storedBidCount() {
		return jdbc.queryForObject("SELECT bid_count FROM auction WHERE id = ?", Integer.class, auctionId);
	}

	private BigDecimal storedCurrentBid() {
		return jdbc.queryForObject("SELECT current_bid FROM auction WHERE id = ?", BigDecimal.class, auctionId);
	}

	private int bidRows() {
		return jdbc.queryForObject("SELECT count(*) FROM bid WHERE auction_id = ?", Integer.class, auctionId);
	}

}
