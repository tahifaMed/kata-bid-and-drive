package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuctionControllerTests {

	private static final String HEADER = "X-User-Id";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void valid_request_creates_an_open_auction_owned_by_the_caller() throws Exception {
		mockMvc.perform(post("/api/auctions").header(HEADER, "1")
						.contentType(MediaType.APPLICATION_JSON).content(validBody(endsIn(Duration.ofDays(2)))))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/auctions/\\d+")))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.seller.displayName").value("Alice"))
				.andExpect(jsonPath("$.startingPrice").value("12000.00"))
				.andExpect(jsonPath("$.currentBid").value("12000.00"))
				.andExpect(jsonPath("$.minNextBid").value("12000.00"))
				.andExpect(jsonPath("$.bidCount").value(0))
				.andExpect(jsonPath("$.highestBidderId").doesNotExist())
				.andExpect(jsonPath("$.bids").isEmpty())
				.andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.matchesPattern(".*/api/auctions/\\d+")))
				.andExpect(jsonPath("$._links.place-bid.href").value(org.hamcrest.Matchers.endsWith("/bids")));
	}

	@Test
	void every_invalid_field_is_listed_in_one_response() throws Exception {
		String body = """
				{"car": {"make": " ", "model": "Golf", "year": 1885, "mileageKm": 0},
				 "startingPrice": "0", "minIncrement": "1.001", "endsAt": "%s"}
				""".formatted(endsIn(Duration.ofDays(2)));

		mockMvc.perform(post("/api/auctions").header(HEADER, "1")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid auction"))
				.andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder(
						"car.make", "car.year", "startingPrice", "minIncrement")));
	}

	@Test
	void missing_mileage_blank_model_and_money_above_the_maximum_are_listed() throws Exception {
		String body = """
				{"car": {"make": "VW", "model": " ", "year": 2020},
				 "startingPrice": "10000000000.00", "minIncrement": "100.00", "endsAt": "%s"}
				""".formatted(endsIn(Duration.ofDays(2)));

		mockMvc.perform(post("/api/auctions").header(HEADER, "1")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder(
						"car.model", "car.mileageKm", "startingPrice")));
	}

	@Test
	void end_time_as_an_epoch_number_is_rejected() throws Exception {
		String body = """
				{"car": {"make": "VW", "model": "Golf", "year": 2020, "mileageKm": 0},
				 "startingPrice": "12000.00", "minIncrement": "100.00", "endsAt": %d}
				""".formatted(Instant.now().plus(Duration.ofDays(2)).getEpochSecond());

		mockMvc.perform(post("/api/auctions").header(HEADER, "1")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void money_as_a_json_number_is_rejected() throws Exception {
		String body = """
				{"car": {"make": "VW", "model": "Golf", "year": 2020, "mileageKm": 0},
				 "startingPrice": 12000.00, "minIncrement": "100.00", "endsAt": "%s"}
				""".formatted(endsIn(Duration.ofDays(2)));

		mockMvc.perform(post("/api/auctions").header(HEADER, "1")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void end_time_outside_the_window_is_a_400_on_end_time() throws Exception {
		assertEndsAtRejected(endsIn(Duration.ofMinutes(59)));
		assertEndsAtRejected(endsIn(Duration.ofDays(30).plusSeconds(1)));
	}

	@Test
	void end_time_at_the_window_bounds_is_accepted() throws Exception {
		mockMvc.perform(post("/api/auctions").header(HEADER, "1")
						.contentType(MediaType.APPLICATION_JSON).content(validBody(endsIn(Duration.ofHours(1).plusSeconds(30)))))
				.andExpect(status().isCreated());
	}

	@Test
	void non_http_image_url_is_rejected() throws Exception {
		String body = """
				{"car": {"make": "VW", "model": "Golf", "year": 2020, "mileageKm": 0, "imageUrl": "ftp://x/y.jpg"},
				 "startingPrice": "12000.00", "minIncrement": "100.00", "endsAt": "%s"}
				""".formatted(endsIn(Duration.ofDays(2)));

		mockMvc.perform(post("/api/auctions").header(HEADER, "1")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.contains("car.imageUrl")));
	}

	@Test
	void missing_identity_is_401() throws Exception {
		mockMvc.perform(post("/api/auctions").contentType(MediaType.APPLICATION_JSON)
						.content(validBody(endsIn(Duration.ofDays(2)))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.title").value("Unauthorized"));
	}

	@Test
	void unknown_or_malformed_identity_is_401_even_with_an_invalid_body() throws Exception {
		for (String header : new String[] { "99", "0", "-1", "1e3", "  " }) {
			mockMvc.perform(post("/api/auctions").header(HEADER, header)
							.contentType(MediaType.APPLICATION_JSON).content("{}"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Test
	void an_end_time_without_an_offset_is_a_400() throws Exception {
		String body = """
				{"car": {"make": "VW", "model": "Golf", "year": 2020, "mileageKm": 0},
				 "startingPrice": "12000.00", "minIncrement": "100.00", "endsAt": "2030-01-01T10:00:00"}
				""";

		mockMvc.perform(post("/api/auctions").header(HEADER, "1")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	private void assertEndsAtRejected(String endsAt) throws Exception {
		mockMvc.perform(post("/api/auctions").header(HEADER, "1")
						.contentType(MediaType.APPLICATION_JSON).content(validBody(endsAt)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.contains("endsAt")));
	}

	private static String endsIn(Duration duration) {
		return Instant.now().plus(duration).toString();
	}

	private static String validBody(String endsAt) {
		return """
				{"car": {"make": "Volkswagen", "model": "Golf", "year": 2020, "mileageKm": 42000,
				         "description": "Blue, one owner"},
				 "startingPrice": "12000.00", "minIncrement": "100.00", "endsAt": "%s"}
				""".formatted(endsAt);
	}

}
