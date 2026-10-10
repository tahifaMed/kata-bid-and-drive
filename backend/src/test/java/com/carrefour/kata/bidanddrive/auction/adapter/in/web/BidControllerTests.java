package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.AuctionNotFoundException;
import com.carrefour.kata.bidanddrive.auction.application.BidNotFoundException;
import com.carrefour.kata.bidanddrive.auction.application.GetBid;
import com.carrefour.kata.bidanddrive.auction.application.InvalidBidException;
import com.carrefour.kata.bidanddrive.auction.application.PlaceBid;
import com.carrefour.kata.bidanddrive.auction.application.RawAmount;
import com.carrefour.kata.bidanddrive.auction.domain.AlreadyHighestBidderException;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionClosedException;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidBelowMinimumException;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.auction.domain.SellerCannotBidException;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BidController.class)
class BidControllerTests {

	private static final UserId BOB = new UserId(2);
	private static final Instant PLACED_AT = Instant.parse("2026-10-11T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PlaceBid placeBid;

	@MockitoBean
	private GetBid getBid;

	@MockitoBean
	private UserDirectory users;

	@BeforeEach
	void bob_exists() {
		given(users.findById(BOB)).willReturn(Optional.of(new User(BOB, "Bob")));
	}

	@Test
	void an_accepted_bid_is_created_with_its_location_and_links() throws Exception {
		given(placeBid.execute(5, BOB, new RawAmount("1000.00")))
				.willReturn(new Bid(12L, 5, BOB, new Money(new BigDecimal("1000.00")), PLACED_AT));

		bid("{\"amount\": \"1000.00\"}")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/auctions/5/bids/12"))
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.auctionId").value(5))
				.andExpect(jsonPath("$.bidderId").value(2))
				.andExpect(jsonPath("$.amount").value("1000.00"))
				.andExpect(jsonPath("$.placedAt").value("2026-10-11T10:00:00Z"))
				.andExpect(jsonPath("$._links.self.href").value("http://localhost/api/auctions/5/bids/12"))
				.andExpect(jsonPath("$._links.auction.href").value("http://localhost/api/auctions/5"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"amount\": 1000}", "{\"amount\": 1000.00}", "{}", "{\"amount\": null}",
			"{\"amount\": {}}"})
	void an_amount_that_is_not_a_json_string_reaches_the_use_case_as_missing(String body) throws Exception {
		given(placeBid.execute(anyLong(), any(), any())).willThrow(new InvalidBidException("amount", "is required"));

		bid(body).andExpect(status().isBadRequest());

		verify(placeBid).execute(5, BOB, new RawAmount(null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"not json", "[]", "\"1000.00\""})
	void a_body_that_is_not_a_json_object_is_invalid_before_the_lookup(String body) throws Exception {
		bid(body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid bid"))
				.andExpect(jsonPath("$.errors").isEmpty());

		verifyNoInteractions(placeBid);
	}

	@Test
	void a_missing_identity_is_unauthorized_before_anything_else() throws Exception {
		mockMvc.perform(post("/api/auctions/5/bids").contentType(MediaType.APPLICATION_JSON).content("not json"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(placeBid);
	}

	@Test
	void an_invalid_amount_is_a_bad_request_with_the_field() throws Exception {
		rejectWith(new InvalidBidException("amount", "must be a positive amount with at most two decimals"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid bid"))
				.andExpect(jsonPath("$.errors[0].field").value("amount"))
				.andExpect(jsonPath("$.errors[0].message").value("must be a positive amount with at most two decimals"));
	}

	@Test
	void an_unknown_auction_is_not_found() throws Exception {
		rejectWith(new AuctionNotFoundException(5))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Auction not found"));
	}

	@Test
	void the_seller_is_forbidden() throws Exception {
		rejectWith(new SellerCannotBidException())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("The seller cannot bid on their own auction"));
	}

	@Test
	void a_closed_auction_is_a_conflict() throws Exception {
		rejectWith(new AuctionClosedException())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("The auction is closed"));
	}

	@Test
	void the_highest_bidder_bidding_again_is_a_conflict() throws Exception {
		rejectWith(new AlreadyHighestBidderException())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("You are already the highest bidder"));
	}

	@Test
	void a_bid_below_the_minimum_is_unprocessable_with_the_minimum_as_a_string() throws Exception {
		rejectWith(new BidBelowMinimumException(new Money(new BigDecimal("1100.00"))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.minimumBid").value("1100.00"))
				.andExpect(jsonPath("$.detail").value("The bid must be at least 1100.00"));
	}

	@Test
	void a_version_conflict_is_a_conflict_to_reload_and_retry() throws Exception {
		rejectWith(new ObjectOptimisticLockingFailureException("AuctionEntity", 5L))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Bid conflict"))
				.andExpect(jsonPath("$.detail").value("bid conflict, reload and retry"));
	}

	@Test
	void the_bidder_reads_their_bid_with_its_links() throws Exception {
		given(getBid.execute(5, 12, BOB))
				.willReturn(new Bid(12L, 5, BOB, new Money(new BigDecimal("1000.00")), PLACED_AT));

		mockMvc.perform(get("/api/auctions/5/bids/12").header("X-User-Id", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.amount").value("1000.00"))
				.andExpect(jsonPath("$._links.self.href").value("http://localhost/api/auctions/5/bids/12"))
				.andExpect(jsonPath("$._links.auction.href").value("http://localhost/api/auctions/5"));
	}

	@Test
	void a_bid_the_caller_cannot_see_is_not_found() throws Exception {
		given(getBid.execute(5, 12, BOB)).willThrow(new BidNotFoundException(5, 12));

		mockMvc.perform(get("/api/auctions/5/bids/12").header("X-User-Id", "2"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Bid not found"));
	}

	@Test
	void reading_a_bid_needs_an_identity() throws Exception {
		mockMvc.perform(get("/api/auctions/5/bids/12")).andExpect(status().isUnauthorized());

		verifyNoInteractions(getBid);
	}

	private ResultActions rejectWith(RuntimeException rejection) throws Exception {
		given(placeBid.execute(anyLong(), any(), any())).willThrow(rejection);
		return bid("{\"amount\": \"1050.00\"}");
	}

	private ResultActions bid(String body) throws Exception {
		return mockMvc.perform(post("/api/auctions/5/bids").header("X-User-Id", "2")
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
