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
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Spec 008 end to end. The database is shared, so each test reads only the items for its own auction. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MyBidsApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuctionRepository auctions;

	@Test
	void a_winning_bidder_shows_as_outbid_once_someone_bids_higher() throws Exception {
		long id = openAuction();
		bid(id, "2", "1000.00").andExpect(status().isCreated());

		myBids("2")
				.andExpect(status().isOk())
				.andExpect(item(id, "status").value(contains("WINNING")))
				.andExpect(item(id, "myHighestBid").value(contains("1000.00")))
				.andExpect(item(id, "currentBid").value(contains("1000.00")));

		bid(id, "4", "1100.00").andExpect(status().isCreated());

		myBids("2")
				.andExpect(item(id, "status").value(contains("OUTBID")))
				.andExpect(item(id, "myHighestBid").value(contains("1000.00")))
				.andExpect(item(id, "currentBid").value(contains("1100.00")));
		myBids("4").andExpect(item(id, "status").value(contains("WINNING")));
	}

	@Test
	void without_an_identity_the_list_is_unauthorized() throws Exception {
		mockMvc.perform(get("/api/me/bids")).andExpect(status().isUnauthorized());
		myBids("999").andExpect(status().isUnauthorized());
	}

	private JsonPathResultMatchers item(long auctionId, String field) {
		return jsonPath("$._embedded.myBids[?(@.auctionId == " + auctionId + ")]." + field);
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
				new Money(new BigDecimal("1000.00")), new Money(new BigDecimal("100.00")),
				Instant.now().plus(Duration.ofDays(1)), AuctionStatus.OPEN)).id();
	}

}
