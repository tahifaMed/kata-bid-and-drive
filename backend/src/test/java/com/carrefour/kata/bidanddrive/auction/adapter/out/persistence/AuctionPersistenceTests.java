package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionWindow;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuctionPersistenceTests {

	private static final AuctionWindow WINDOW = new AuctionWindow(Duration.ofHours(1), Duration.ofDays(30));

	private final JpaAuctionRepository auctions;
	private final JdbcTemplate jdbc;

	@Autowired
	AuctionPersistenceTests(JpaAuctionRepository auctions, JdbcTemplate jdbc) {
		this.auctions = auctions;
		this.jdbc = jdbc;
	}

	@Test
	void saving_an_auction_stores_one_auction_row_and_its_car() {
		int auctionsBefore = countAuctions();
		// TIMESTAMPTZ stores microseconds; Linux clocks can give nanoseconds.
		Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
		Car car = new Car("Volkswagen", "Golf", 2020, 42000, "Blue, one owner", "https://example.com/golf.jpg");
		Auction auction = Auction.open(new UserId(1), car, new BigDecimal("12000.00"), new BigDecimal("100.00"),
				now.plus(Duration.ofDays(2)), now, WINDOW);

		Auction saved = auctions.save(auction);

		assertThat(saved.id()).isNotNull();
		Auction found = auctions.findById(saved.id()).orElseThrow();
		assertThat(found.car()).isEqualTo(car);
		assertThat(found.sellerId()).isEqualTo(new UserId(1));
		assertThat(found.startingPrice().amount()).isEqualByComparingTo("12000.00");
		assertThat(found.minIncrement().amount()).isEqualByComparingTo("100.00");
		assertThat(found.endsAt()).isEqualTo(auction.endsAt());
		assertThat(countAuctions()).isEqualTo(auctionsBefore + 1);
		assertThat(countCarsFor(saved.id())).isEqualTo(1);
	}

	@Test
	void a_second_car_for_the_same_auction_is_rejected() {
		Instant now = Instant.now();
		Auction saved = auctions.save(Auction.open(new UserId(2),
				new Car("Peugeot", "208", 2019, 1000, null, null), BigDecimal.TEN, BigDecimal.ONE,
				now.plus(Duration.ofDays(1)), now, WINDOW));

		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO car (auction_id, make, model, year, mileage_km) VALUES (?, 'Renault', 'Clio', 2018, 0)",
				saved.id()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void find_by_id_returns_an_auction_that_has_already_ended() {
		Instant ended = Instant.now().minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.MICROS);
		Auction saved = auctions.save(new Auction(null, new UserId(1),
				new Car("Fiat", "Panda", 2015, 90000, null, null),
				new Money(new BigDecimal("3000.00")), new Money(new BigDecimal("50.00")),
				ended, AuctionStatus.OPEN));

		Auction found = auctions.findById(saved.id()).orElseThrow();

		assertThat(found.endsAt()).isEqualTo(ended);
		assertThat(found.car().make()).isEqualTo("Fiat");
	}

	@Test
	void find_by_id_of_an_unknown_auction_is_empty() {
		assertThat(auctions.findById(Long.MAX_VALUE)).isEmpty();
	}

	private int countAuctions() {
		return jdbc.queryForObject("SELECT count(*) FROM auction", Integer.class);
	}

	private int countCarsFor(long auctionId) {
		return jdbc.queryForObject("SELECT count(*) FROM car WHERE auction_id = ?", Integer.class, auctionId);
	}

}
