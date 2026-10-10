package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.application.AuctionRepository;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Spec 007 end to end, against the real database. Each test makes its own auction, sold by user 1. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlaceBidApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuctionRepository auctions;

	@Test
	void a_first_bid_at_the_starting_price_is_created_and_updates_the_auction() throws Exception {
		long id = openAuction();

		bid(id, "2", "\"1000.00\"")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.bidderId").value(2))
				.andExpect(jsonPath("$.amount").value("1000.00"));

		mockMvc.perform(get("/api/auctions/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentBid").value("1000.00"))
				.andExpect(jsonPath("$.minNextBid").value("1100.00"))
				.andExpect(jsonPath("$.bidCount").value(1))
				.andExpect(jsonPath("$.highestBidderId").value(2))
				.andExpect(jsonPath("$.bids[0].amount").value("1000.00"))
				.andExpect(jsonPath("$.bids[0].bidderName").value("B***"));
	}

	@Test
	void a_bid_below_current_plus_increment_is_rejected_with_the_minimum_then_the_minimum_is_accepted()
			throws Exception {
		long id = openAuction();
		bid(id, "2", "\"1000.00\"").andExpect(status().isCreated());

		bid(id, "3", "\"1050.00\"")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.minimumBid").value("1100.00"));
		bid(id, "3", "\"1100.00\"").andExpect(status().isCreated());
	}

	@Test
	void the_seller_cannot_bid() throws Exception {
		bid(openAuction(), "1", "\"1000.00\"").andExpect(status().isForbidden());
	}

	@Test
	void the_highest_bidder_cannot_bid_again() throws Exception {
		long id = openAuction();
		bid(id, "2", "\"1000.00\"").andExpect(status().isCreated());

		bid(id, "2", "\"1200.00\"")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("You are already the highest bidder"));
	}

	@Test
	void an_ended_auction_is_closed_even_for_a_bid_below_the_minimum() throws Exception {
		long id = auction(Instant.now().minus(Duration.ofMinutes(1)));

		bid(id, "3", "\"1.00\"")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("The auction is closed"));
	}

	@Test
	void an_amount_sent_as_a_json_number_is_a_bad_request() throws Exception {
		bid(openAuction(), "2", "1000.00")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid bid"))
				.andExpect(jsonPath("$.errors[0].field").value("amount"));
	}

	@Test
	void an_amount_with_three_significant_decimals_is_a_bad_request() throws Exception {
		bid(openAuction(), "2", "\"1000.001\"").andExpect(status().isBadRequest());
	}

	@Test
	void an_unknown_user_is_unauthorized() throws Exception {
		bid(openAuction(), "999", "\"1000.00\"").andExpect(status().isUnauthorized());
	}

	@Test
	void an_unknown_auction_is_not_found_even_with_an_invalid_amount() throws Exception {
		bid(Long.MAX_VALUE, "2", "1100").andExpect(status().isNotFound());
		mockMvc.perform(post("/api/auctions/{id}/bids", Long.MAX_VALUE).header("X-User-Id", "2")
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void the_same_bid_posted_twice_is_created_once_then_already_highest() throws Exception {
		long id = openAuction();

		bid(id, "2", "\"1000.00\"").andExpect(status().isCreated());
		bid(id, "2", "\"1000.00\"").andExpect(status().isConflict());

		mockMvc.perform(get("/api/auctions/{id}", id)).andExpect(jsonPath("$.bidCount").value(1));
	}

	private ResultActions bid(long auctionId, String userId, String amountJson) throws Exception {
		return mockMvc.perform(post("/api/auctions/{id}/bids", auctionId).header("X-User-Id", userId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"amount\": " + amountJson + "}"));
	}

	private long openAuction() {
		return auction(Instant.now().plus(Duration.ofDays(1)));
	}

	private long auction(Instant endsAt) {
		return auctions.save(new Auction(null, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
				new Money(new BigDecimal("1000.00")), new Money(new BigDecimal("100.00")), endsAt,
				AuctionStatus.OPEN)).id();
	}

}
