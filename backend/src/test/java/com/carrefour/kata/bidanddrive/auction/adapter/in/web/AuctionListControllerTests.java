package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.AuctionPage;
import com.carrefour.kata.bidanddrive.auction.application.CreateAuction;
import com.carrefour.kata.bidanddrive.auction.application.ListOpenAuctions;
import com.carrefour.kata.bidanddrive.auction.application.PageQuery;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuctionController.class)
class AuctionListControllerTests {

	private static final Instant FIRST_END = Instant.parse("2026-10-12T18:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListOpenAuctions listOpenAuctions;

	@MockitoBean
	private CreateAuction createAuction;

	@MockitoBean
	private UserDirectory users;

	@Test
	void each_item_has_id_car_money_bid_count_end_time_and_self_link() throws Exception {
		givenPage(new PageQuery(0, 20), List.of(auction(1)), false);

		mockMvc.perform(get("/api/auctions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auctions[0].id").value(1))
				.andExpect(jsonPath("$._embedded.auctions[0].car.make").value("Volkswagen"))
				.andExpect(jsonPath("$._embedded.auctions[0].car.model").value("Golf"))
				.andExpect(jsonPath("$._embedded.auctions[0].car.year").value(2020))
				.andExpect(jsonPath("$._embedded.auctions[0].car.imageUrl").value("https://example.com/golf.jpg"))
				.andExpect(jsonPath("$._embedded.auctions[0].car.mileageKm").doesNotExist())
				.andExpect(jsonPath("$._embedded.auctions[0].currentBid").value("12000.00"))
				.andExpect(jsonPath("$._embedded.auctions[0].minNextBid").value("12000.00"))
				.andExpect(jsonPath("$._embedded.auctions[0].bidCount").value(0))
				.andExpect(jsonPath("$._embedded.auctions[0].endsAt").value("2026-10-12T18:00:00Z"))
				.andExpect(jsonPath("$._embedded.auctions[0]._links.self.href").value(endsWith("/api/auctions/1")));
	}

	@Test
	void a_middle_page_links_to_its_neighbours_and_the_page_is_described() throws Exception {
		givenPage(new PageQuery(1, 2), List.of(auction(3), auction(4)), true);

		mockMvc.perform(get("/api/auctions").param("page", "1").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auctions[*].id").value(contains(3, 4)))
				.andExpect(jsonPath("$.page.number").value(1))
				.andExpect(jsonPath("$.page.size").value(2))
				.andExpect(jsonPath("$._links.self.href").value(endsWith("/api/auctions?page=1&size=2")))
				.andExpect(jsonPath("$._links.prev.href").value(endsWith("/api/auctions?page=0&size=2")))
				.andExpect(jsonPath("$._links.next.href").value(endsWith("/api/auctions?page=2&size=2")));
	}

	@Test
	void the_first_page_has_no_prev_link() throws Exception {
		givenPage(new PageQuery(0, 2), List.of(auction(1), auction(2)), true);

		mockMvc.perform(get("/api/auctions").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.prev").doesNotExist())
				.andExpect(jsonPath("$._links.next.href").value(endsWith("/api/auctions?page=1&size=2")));
	}

	@Test
	void the_last_page_has_no_next_link() throws Exception {
		givenPage(new PageQuery(2, 2), List.of(auction(5)), false);

		mockMvc.perform(get("/api/auctions").param("page", "2").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.prev.href").value(endsWith("/api/auctions?page=1&size=2")))
				.andExpect(jsonPath("$._links.next").doesNotExist());
	}

	@Test
	void with_no_auctions_the_list_is_empty_and_still_present() throws Exception {
		givenPage(new PageQuery(0, 20), List.of(), false);

		mockMvc.perform(get("/api/auctions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auctions").isArray())
				.andExpect(jsonPath("$._embedded.auctions").isEmpty());
	}

	@Test
	void without_paging_parameters_the_first_page_of_twenty_is_listed() throws Exception {
		givenPage(new PageQuery(0, 20), List.of(), false);

		mockMvc.perform(get("/api/auctions")).andExpect(status().isOk());

		verify(listOpenAuctions).execute(new PageQuery(0, 20));
	}

	@ParameterizedTest
	@CsvSource({"size=0,size", "size=101,size", "page=-1,page"})
	void paging_outside_its_bounds_is_a_400_naming_the_field(String query, String field) throws Exception {
		mockMvc.perform(get("/api/auctions?" + query))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid auction"))
				.andExpect(jsonPath("$.errors[*].field").value(contains(field)));

		verifyNoInteractions(listOpenAuctions);
	}

	@Test
	void a_paging_value_that_is_not_a_number_is_a_400_naming_the_field() throws Exception {
		mockMvc.perform(get("/api/auctions").param("page", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("page")));

		verifyNoInteractions(listOpenAuctions);
	}

	@Test
	void the_list_needs_no_identity_and_ignores_an_unknown_one() throws Exception {
		givenPage(new PageQuery(0, 20), List.of(), false);

		mockMvc.perform(get("/api/auctions")).andExpect(status().isOk());
		mockMvc.perform(get("/api/auctions").header("X-User-Id", "999")).andExpect(status().isOk());
	}

	private void givenPage(PageQuery query, List<Auction> items, boolean hasNext) {
		given(listOpenAuctions.execute(query)).willReturn(new AuctionPage<>(items, query.number(), query.size(), hasNext));
	}

	private static Auction auction(long id) {
		return new Auction(id, new UserId(1),
				new Car("Volkswagen", "Golf", 2020, 42000, "Blue, one owner", "https://example.com/golf.jpg"),
				new Money(new BigDecimal("12000.00")), new Money(new BigDecimal("100.00")),
				FIRST_END.plus(Duration.ofHours(id - 1)), AuctionStatus.OPEN);
	}

}
