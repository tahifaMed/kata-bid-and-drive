package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.application.AuctionRepository;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec 011 end to end: the result is worked out on read from the injected clock, which this test moves.
 * The database is shared, so each test reads only its own auction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AuctionResultApiTests.ClockConfig.class})
class AuctionResultApiTests {

	private static final Instant START = Instant.parse("2100-01-01T12:00:00Z");
	private static final Instant ENDS_AT = START.plus(Duration.ofDays(1));

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuctionRepository auctions;

	@Autowired
	private MutableClock clock;

	@BeforeEach
	void start_before_the_end() {
		clock.set(START);
	}

	@Test
	void an_ended_auction_is_sold_to_the_highest_bidder_who_won_while_the_others_lost() throws Exception {
		long id = openAuction();
		bid(id, "2", "1000.00").andExpect(status().isCreated());
		bid(id, "3", "1100.00").andExpect(status().isCreated());

		clock.set(ENDS_AT);

		mockMvc.perform(get("/api/auctions/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"))
				.andExpect(jsonPath("$.outcome").value("SOLD"))
				.andExpect(jsonPath("$.winnerName").value("C***"))
				.andExpect(jsonPath("$.highestBidderId").value(3))
				.andExpect(jsonPath("$.currentBid").value("1100.00"))
				.andExpect(jsonPath("$._links.place-bid").doesNotExist());
		myBids("3").andExpect(item(id, "status").value(contains("WON")));
		myBids("2").andExpect(item(id, "status").value(contains("LOST")));
		detail(id, "3").andExpect(jsonPath("$.myBid.status").value("WON"));
		detail(id, "2").andExpect(jsonPath("$.myBid.status").value("LOST"));
		bid(id, "2", "1200.00").andExpect(status().isConflict());
	}

	@Test
	void an_ended_auction_without_bids_has_no_winner() throws Exception {
		long id = openAuction();

		clock.set(ENDS_AT.plusSeconds(60));

		mockMvc.perform(get("/api/auctions/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"))
				.andExpect(jsonPath("$.outcome").value("NO_BIDS"))
				.andExpect(jsonPath("$", not(hasKey("winnerName"))))
				.andExpect(jsonPath("$.highestBidderId").doesNotExist())
				.andExpect(jsonPath("$.currentBid").value("1000.00"))
				.andExpect(jsonPath("$._links.place-bid").doesNotExist());
	}

	@Test
	void one_millisecond_before_the_end_the_auction_is_still_open_everywhere() throws Exception {
		long id = openAuction();
		bid(id, "2", "1000.00").andExpect(status().isCreated());
		bid(id, "3", "1100.00").andExpect(status().isCreated());

		clock.set(ENDS_AT.minusMillis(1));

		detail(id, "2")
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$", not(hasKey("outcome"))))
				.andExpect(jsonPath("$._links.place-bid").exists())
				.andExpect(jsonPath("$.myBid.status").value("OUTBID"));
		myBids("3").andExpect(item(id, "status").value(contains("WINNING")));
		myBids("2").andExpect(item(id, "status").value(contains("OUTBID")));
	}

	@Test
	void at_the_end_time_exactly_the_auction_is_closed_and_refuses_bids() throws Exception {
		long id = openAuction();

		clock.set(ENDS_AT);

		mockMvc.perform(get("/api/auctions/{id}", id)).andExpect(jsonPath("$.status").value("CLOSED"));
		bid(id, "2", "1000.00").andExpect(status().isConflict());
	}

	private JsonPathResultMatchers item(long auctionId, String field) {
		return jsonPath("$._embedded.myBids[?(@.auctionId == " + auctionId + ")]." + field);
	}

	private ResultActions detail(long auctionId, String userId) throws Exception {
		return mockMvc.perform(get("/api/auctions/{id}", auctionId).header("X-User-Id", userId));
	}

	private ResultActions myBids(String userId) throws Exception {
		return mockMvc.perform(get("/api/me/bids").header("X-User-Id", userId));
	}

	private ResultActions bid(long auctionId, String userId, String amount) throws Exception {
		return mockMvc.perform(post("/api/auctions/{id}/bids", auctionId).header("X-User-Id", userId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"amount\": \"" + amount + "\"}"));
	}

	private long openAuction() {
		return auctions.save(new Auction(null, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
				new Money(new BigDecimal("1000.00")), new Money(new BigDecimal("100.00")), ENDS_AT,
				AuctionStatus.OPEN)).id();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ClockConfig {

		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(START);
		}

	}

	/** A clock the test moves past the end time. */
	static final class MutableClock extends Clock {

		private volatile Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void set(Instant now) {
			this.now = now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

}
