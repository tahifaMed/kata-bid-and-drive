package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionTests {

	private static final Instant NOW = ZonedDateTime.of(2026, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
	private static final AuctionWindow WINDOW = new AuctionWindow(Duration.ofHours(1), Duration.ofDays(30));
	private static final UserId ALICE = new UserId(1);

	@Test
	void opens_with_status_open_and_no_id() {
		Auction auction = open(NOW.plus(Duration.ofDays(1)));

		assertThat(auction.id()).isNull();
		assertThat(auction.status()).isEqualTo(AuctionStatus.OPEN);
		assertThat(auction.sellerId()).isEqualTo(ALICE);
		assertThat(auction.startingPrice().amount()).isEqualByComparingTo("12000.00");
		assertThat(auction.minIncrement().amount()).isEqualTo(new BigDecimal("100.00"));
	}

	@Test
	void rejects_end_one_second_before_the_minimum_window() {
		assertInvalid("endsAt", () -> open(NOW.plus(Duration.ofHours(1)).minusSeconds(1)));
	}

	@Test
	void accepts_end_exactly_one_hour_from_now() {
		assertThat(open(NOW.plus(Duration.ofHours(1))).endsAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
	}

	@Test
	void accepts_end_exactly_thirty_days_from_now() {
		assertThat(open(NOW.plus(Duration.ofDays(30))).endsAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
	}

	@Test
	void rejects_end_one_second_after_the_maximum_window() {
		assertInvalid("endsAt", () -> open(NOW.plus(Duration.ofDays(30)).plusSeconds(1)));
	}

	@Test
	void rejects_missing_end() {
		assertInvalid("endsAt", () -> open(null));
	}

	@Test
	void window_bounds_come_from_configuration() {
		AuctionWindow shortWindow = new AuctionWindow(Duration.ofMinutes(5), Duration.ofHours(2));

		assertThat(Auction.open(ALICE, car(2020), BigDecimal.TEN, BigDecimal.ONE,
				NOW.plus(Duration.ofMinutes(5)), NOW, shortWindow).status()).isEqualTo(AuctionStatus.OPEN);
		assertInvalid("endsAt", () -> Auction.open(ALICE, car(2020), BigDecimal.TEN, BigDecimal.ONE,
				NOW.plus(Duration.ofHours(3)), NOW, shortWindow));
	}

	@Test
	void year_bounds_use_the_clock_year() {
		assertThat(open(NOW.plus(Duration.ofDays(1)), car(1886)).car().year()).isEqualTo(1886);
		assertThat(open(NOW.plus(Duration.ofDays(1)), car(2027)).car().year()).isEqualTo(2027);
		assertInvalid("car.year", () -> open(NOW.plus(Duration.ofDays(1)), car(1885)));
		assertInvalid("car.year", () -> open(NOW.plus(Duration.ofDays(1)), car(2028)));
	}

	@Test
	void rejects_non_positive_starting_price() {
		assertInvalid("startingPrice", () -> Auction.open(ALICE, car(2020), BigDecimal.ZERO, BigDecimal.ONE,
				NOW.plus(Duration.ofDays(1)), NOW, WINDOW));
	}

	@Test
	void rejects_non_positive_min_increment() {
		assertInvalid("minIncrement", () -> Auction.open(ALICE, car(2020), BigDecimal.TEN, new BigDecimal("-1"),
				NOW.plus(Duration.ofDays(1)), NOW, WINDOW));
	}

	@Test
	void rejects_money_with_more_than_two_decimals() {
		assertInvalid("startingPrice", () -> Auction.open(ALICE, car(2020), new BigDecimal("1.001"), BigDecimal.ONE,
				NOW.plus(Duration.ofDays(1)), NOW, WINDOW));
	}

	@Test
	void accepts_trailing_zeros_beyond_two_decimals() {
		Auction auction = Auction.open(ALICE, car(2020), new BigDecimal("12000.000"), BigDecimal.ONE,
				NOW.plus(Duration.ofDays(1)), NOW, WINDOW);

		assertThat(auction.startingPrice().amount()).isEqualTo(new BigDecimal("12000.00"));
	}

	@Test
	void rejects_money_above_column_precision() {
		assertInvalid("startingPrice", () -> Auction.open(ALICE, car(2020), new BigDecimal("10000000000.00"),
				BigDecimal.ONE, NOW.plus(Duration.ofDays(1)), NOW, WINDOW));
	}

	@Test
	void rejects_blank_make_and_long_model() {
		assertThatThrownBy(() -> new Car(" ", "Golf", 2020, 0, null, null))
				.isInstanceOfSatisfying(InvalidAuctionException.class,
						e -> assertThat(e.field()).isEqualTo("car.make"));
		assertThatThrownBy(() -> new Car("VW", "x".repeat(61), 2020, 0, null, null))
				.isInstanceOfSatisfying(InvalidAuctionException.class,
						e -> assertThat(e.field()).isEqualTo("car.model"));
	}

	@Test
	void rejects_negative_mileage() {
		assertThatThrownBy(() -> new Car("VW", "Golf", 2020, -1, null, null))
				.isInstanceOfSatisfying(InvalidAuctionException.class,
						e -> assertThat(e.field()).isEqualTo("car.mileageKm"));
	}

	@Test
	void rejects_description_over_2000_characters() {
		assertThatThrownBy(() -> new Car("VW", "Golf", 2020, 0, "x".repeat(2001), null))
				.isInstanceOfSatisfying(InvalidAuctionException.class,
						e -> assertThat(e.field()).isEqualTo("car.description"));
	}

	@Test
	void accepts_http_and_https_image_urls_only() {
		new Car("VW", "Golf", 2020, 0, null, "https://example.com/car.jpg");
		new Car("VW", "Golf", 2020, 0, null, "http://example.com/car.jpg");
		assertInvalidImage("ftp://example.com/car.jpg");
		assertInvalidImage("javascript:alert(1)");
		assertInvalidImage("not a url");
		assertInvalidImage("https://" + "x".repeat(2048));
	}

	private static void assertInvalidImage(String url) {
		assertThatThrownBy(() -> new Car("VW", "Golf", 2020, 0, null, url))
				.isInstanceOfSatisfying(InvalidAuctionException.class,
						e -> assertThat(e.field()).isEqualTo("car.imageUrl"));
	}

	private static Auction open(Instant endsAt) {
		return open(endsAt, car(2020));
	}

	private static Auction open(Instant endsAt, Car car) {
		return Auction.open(ALICE, car, new BigDecimal("12000.00"), new BigDecimal("100.00"), endsAt, NOW, WINDOW);
	}

	private static Car car(int year) {
		return new Car("Volkswagen", "Golf", year, 42000, null, null);
	}

	private static void assertInvalid(String field, Runnable action) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(InvalidAuctionException.class,
						e -> assertThat(e.field()).isEqualTo(field));
	}

}
