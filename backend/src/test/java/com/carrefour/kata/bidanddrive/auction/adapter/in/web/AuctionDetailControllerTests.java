package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.AuctionDetail;
import com.carrefour.kata.bidanddrive.auction.application.AuctionNotFoundException;
import com.carrefour.kata.bidanddrive.auction.application.CreateAuction;
import com.carrefour.kata.bidanddrive.auction.application.GetAuctionDetail;
import com.carrefour.kata.bidanddrive.auction.application.ListOpenAuctions;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuctionController.class)
class AuctionDetailControllerTests {

	private static final Instant ENDS_AT = Instant.parse("2026-10-12T18:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetAuctionDetail getAuctionDetail;

	@MockitoBean
	private ListOpenAuctions listOpenAuctions;

	@MockitoBean
	private CreateAuction createAuction;

	@MockitoBean
	private UserDirectory users;

	@Test
	void shows_the_car_prices_seller_status_and_links() throws Exception {
		given(getAuctionDetail.execute(5)).willReturn(new AuctionDetail(auction(), "Alice", List.of(), true));

		mockMvc.perform(get("/api/auctions/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.car.make").value("Volkswagen"))
				.andExpect(jsonPath("$.car.model").value("Golf"))
				.andExpect(jsonPath("$.car.year").value(2020))
				.andExpect(jsonPath("$.car.mileageKm").value(42000))
				.andExpect(jsonPath("$.car.description").value("Blue, one owner"))
				.andExpect(jsonPath("$.car.imageUrl").value("https://example.com/golf.jpg"))
				.andExpect(jsonPath("$.seller.displayName").value("Alice"))
				.andExpect(jsonPath("$.seller.id").doesNotExist())
				.andExpect(jsonPath("$.sellerId").value(1))
				.andExpect(jsonPath("$.startingPrice").value("12000.00"))
				.andExpect(jsonPath("$.minIncrement").value("100.00"))
				.andExpect(jsonPath("$.endsAt").value("2026-10-12T18:00:00Z"))
				.andExpect(jsonPath("$._links.self.href").value(endsWith("/api/auctions/5")))
				.andExpect(jsonPath("$._links.place-bid.href").value(endsWith("/api/auctions/5/bids")));
	}

	@Test
	void with_no_bids_the_prices_are_the_starting_price_and_the_history_is_empty() throws Exception {
		given(getAuctionDetail.execute(5)).willReturn(new AuctionDetail(auction(), "Alice", List.of(), true));

		mockMvc.perform(get("/api/auctions/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentBid").value("12000.00"))
				.andExpect(jsonPath("$.minNextBid").value("12000.00"))
				.andExpect(jsonPath("$.bidCount").value(0))
				.andExpect(jsonPath("$.highestBidderId").value(nullValue()))
				.andExpect(jsonPath("$.bids").isEmpty());
	}

	@Test
	void lists_the_bids_in_the_given_order_with_masked_names_and_no_bidder_ids() throws Exception {
		List<AuctionDetail.HistoryEntry> bids = List.of(
				entry("12200.00", "2026-10-11T10:02:00Z", "B***"),
				entry("12100.00", "2026-10-11T10:01:00Z", "C***"),
				entry("12000.00", "2026-10-11T10:00:00Z", "B***"));
		given(getAuctionDetail.execute(5)).willReturn(new AuctionDetail(auction(), "Alice", bids, true));

		mockMvc.perform(get("/api/auctions/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bids.length()").value(3))
				.andExpect(jsonPath("$.bids[0].amount").value("12200.00"))
				.andExpect(jsonPath("$.bids[0].placedAt").value("2026-10-11T10:02:00Z"))
				.andExpect(jsonPath("$.bids[0].bidderName").value("B***"))
				.andExpect(jsonPath("$.bids[1].bidderName").value("C***"))
				.andExpect(jsonPath("$.bids[2].amount").value("12000.00"))
				.andExpect(jsonPath("$.bids[0].bidderId").doesNotExist());
	}

	@Test
	void an_auction_that_is_not_biddable_has_no_place_bid_link_and_keeps_its_status() throws Exception {
		given(getAuctionDetail.execute(5)).willReturn(new AuctionDetail(auction(), "Alice", List.of(), false));

		mockMvc.perform(get("/api/auctions/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$._links.self.href").value(endsWith("/api/auctions/5")))
				.andExpect(jsonPath("$._links.place-bid").doesNotExist());
	}

	@Test
	void an_unknown_auction_is_a_404_problem() throws Exception {
		given(getAuctionDetail.execute(99)).willThrow(new AuctionNotFoundException(99));

		mockMvc.perform(get("/api/auctions/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Auction not found"))
				.andExpect(jsonPath("$.detail").value("Auction 99 does not exist"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"0", "-1"})
	void zero_and_negative_ids_are_looked_up_and_not_found(String id) throws Exception {
		long value = Long.parseLong(id);
		given(getAuctionDetail.execute(value)).willThrow(new AuctionNotFoundException(value));

		mockMvc.perform(get("/api/auctions/" + id))
				.andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc", "1.5", "9223372036854775808"})
	void an_id_that_is_not_a_64_bit_whole_number_is_a_400_naming_id(String id) throws Exception {
		mockMvc.perform(get("/api/auctions/" + id))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid auction"))
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0].field").value("id"))
				.andExpect(jsonPath("$.errors[0].message").value("must be a whole number"));

		verifyNoInteractions(getAuctionDetail);
	}

	@Test
	void broken_data_is_a_500_problem_that_hides_the_cause() throws Exception {
		given(getAuctionDetail.execute(5)).willThrow(new IllegalStateException("user 42 does not exist"));

		mockMvc.perform(get("/api/auctions/5"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.title").value("Internal error"))
				.andExpect(jsonPath("$.detail").value("The request could not be completed"))
				.andExpect(jsonPath("$.trace").doesNotExist())
				.andExpect(jsonPath("$.exception").doesNotExist())
				.andExpect(content().string(not(containsString("user 42"))));
	}

	@ParameterizedTest
	@ValueSource(strings = {"999", "not-a-number"})
	void the_detail_is_public_and_ignores_an_unknown_or_malformed_identity(String identity) throws Exception {
		given(getAuctionDetail.execute(5)).willReturn(new AuctionDetail(auction(), "Alice", List.of(), true));

		mockMvc.perform(get("/api/auctions/5")).andExpect(status().isOk());
		mockMvc.perform(get("/api/auctions/5").header("X-User-Id", identity)).andExpect(status().isOk());
	}

	private static Auction auction() {
		return new Auction(5L, new UserId(1),
				new Car("Volkswagen", "Golf", 2020, 42000, "Blue, one owner", "https://example.com/golf.jpg"),
				money("12000.00"), money("100.00"), ENDS_AT, AuctionStatus.OPEN);
	}

	private static AuctionDetail.HistoryEntry entry(String amount, String placedAt, String bidderName) {
		return new AuctionDetail.HistoryEntry(money(amount), Instant.parse(placedAt), bidderName);
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

}
