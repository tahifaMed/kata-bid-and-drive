package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.ListMyBids;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MyBidsController.class)
class MyBidsControllerTests {

	private static final UserId BOB = new UserId(2);
	private static final UserId CHLOE = new UserId(3);
	private static final UserId DAN = new UserId(4);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListMyBids listMyBids;

	@MockitoBean
	private UserDirectory users;

	@BeforeEach
	void users_exist() {
		given(users.findById(BOB)).willReturn(Optional.of(new User(BOB, "Bob")));
		given(users.findById(CHLOE)).willReturn(Optional.of(new User(CHLOE, "Chloe")));
	}

	@Test
	void each_item_has_the_auction_car_both_amounts_status_end_and_a_link_to_the_auction() throws Exception {
		given(listMyBids.execute(BOB)).willReturn(List.of(
				new BidderStanding(auction(6, "2026-10-12T18:00:00Z", "1000.00", DAN), BOB, money("900.00")),
				new BidderStanding(auction(5, "2026-10-13T18:00:00Z", "1200.00", BOB), BOB, money("1200.00"))));

		mockMvc.perform(get("/api/me/bids").header("X-User-Id", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.myBids[*].auctionId").value(contains(6, 5)))
				.andExpect(jsonPath("$._embedded.myBids[0].car.make").value("Volkswagen"))
				.andExpect(jsonPath("$._embedded.myBids[0].car.model").value("Golf"))
				.andExpect(jsonPath("$._embedded.myBids[0].car.year").value(2020))
				.andExpect(jsonPath("$._embedded.myBids[0].car.imageUrl").value("https://example.com/golf.jpg"))
				.andExpect(jsonPath("$._embedded.myBids[0].car.mileageKm").doesNotExist())
				.andExpect(jsonPath("$._embedded.myBids[0].myHighestBid").value("900.00"))
				.andExpect(jsonPath("$._embedded.myBids[0].currentBid").value("1000.00"))
				.andExpect(jsonPath("$._embedded.myBids[0].status").value("OUTBID"))
				.andExpect(jsonPath("$._embedded.myBids[0].endsAt").value("2026-10-12T18:00:00Z"))
				.andExpect(jsonPath("$._embedded.myBids[0]._links.auction.href")
						.value("http://localhost/api/auctions/6"))
				.andExpect(jsonPath("$._embedded.myBids[1].myHighestBid").value("1200.00"))
				.andExpect(jsonPath("$._embedded.myBids[1].currentBid").value("1200.00"))
				.andExpect(jsonPath("$._embedded.myBids[1].status").value("WINNING"))
				.andExpect(jsonPath("$._links.self.href").value("http://localhost/api/me/bids"));
	}

	@Test
	void a_user_who_never_bid_gets_an_empty_list() throws Exception {
		given(listMyBids.execute(CHLOE)).willReturn(List.of());

		mockMvc.perform(get("/api/me/bids").header("X-User-Id", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.myBids").isArray())
				.andExpect(jsonPath("$._embedded.myBids").isEmpty())
				.andExpect(jsonPath("$._links.self.href").value("http://localhost/api/me/bids"));
	}

	@Test
	void without_an_identity_the_list_is_unauthorized() throws Exception {
		mockMvc.perform(get("/api/me/bids"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		verifyNoInteractions(listMyBids);
	}

	@Test
	void an_unknown_user_is_unauthorized() throws Exception {
		given(users.findById(new UserId(999))).willReturn(Optional.empty());

		mockMvc.perform(get("/api/me/bids").header("X-User-Id", "999")).andExpect(status().isUnauthorized());

		verifyNoInteractions(listMyBids);
	}

	@Test
	void a_malformed_id_is_unauthorized_without_looking_up_users() throws Exception {
		mockMvc.perform(get("/api/me/bids").header("X-User-Id", "abc")).andExpect(status().isUnauthorized());

		verifyNoInteractions(listMyBids);
		verify(users, never()).findById(any());
	}

	private static Auction auction(long id, String endsAt, String leadingAmount, UserId leader) {
		return new Auction(id, new UserId(1),
				new Car("Volkswagen", "Golf", 2020, 42000, "Blue, one owner", "https://example.com/golf.jpg"),
				money("800.00"), money("100.00"), Instant.parse(endsAt), AuctionStatus.OPEN,
				money(leadingAmount), leader, 2, 2);
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

}
