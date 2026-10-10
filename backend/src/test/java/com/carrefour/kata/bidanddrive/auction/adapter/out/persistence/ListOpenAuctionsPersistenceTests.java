package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.application.AuctionPage;
import com.carrefour.kata.bidanddrive.auction.application.PageQuery;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionWindow;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each test runs in a transaction that is rolled back, so the rows of one test never reach another.
 * The list is read "as of" 2100-01-01, far from the rows other test classes create.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ListOpenAuctionsPersistenceTests {

	private static final AuctionWindow WINDOW = new AuctionWindow(Duration.ofHours(1), Duration.ofDays(30));
	private static final Instant CREATED_AT = Instant.parse("2099-12-31T00:00:00Z");
	private static final Instant NOW = Instant.parse("2100-01-01T00:00:00Z");
	private static final Car CAR = new Car("Volkswagen", "Golf", 2020, 42000, null, null);

	private final JpaAuctionRepository auctions;

	@Autowired
	ListOpenAuctionsPersistenceTests(JpaAuctionRepository auctions) {
		this.auctions = auctions;
	}

	@Test
	void open_auctions_are_listed_by_end_time_ascending() {
		Auction latest = open(NOW.plus(Duration.ofHours(3)));
		Auction earliest = open(NOW.plus(Duration.ofHours(1)));
		Auction middle = open(NOW.plus(Duration.ofHours(2)));

		AuctionPage<Auction> page = auctions.findOpen(NOW, new PageQuery(0, 10));

		assertThat(ids(page)).containsExactly(earliest.id(), middle.id(), latest.id());
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void auctions_with_the_same_end_time_are_listed_by_id() {
		Instant endsAt = NOW.plus(Duration.ofHours(1));
		Auction first = open(endsAt);
		Auction second = open(endsAt);

		AuctionPage<Auction> page = auctions.findOpen(NOW, new PageQuery(0, 10));

		assertThat(ids(page)).containsExactly(first.id(), second.id());
	}

	@Test
	void an_open_auction_whose_end_time_has_passed_is_not_listed() {
		Auction ended = open(CREATED_AT.plus(Duration.ofHours(2)));
		Auction running = open(NOW.plus(Duration.ofHours(1)));

		AuctionPage<Auction> page = auctions.findOpen(NOW, new PageQuery(0, 10));

		assertThat(ids(page)).containsExactly(running.id()).doesNotContain(ended.id());
	}

	@Test
	void with_no_open_auctions_the_page_is_empty() {
		open(NOW.plus(Duration.ofHours(1)));

		AuctionPage<Auction> page = auctions.findOpen(Instant.parse("2200-01-01T00:00:00Z"), new PageQuery(0, 20));

		assertThat(page.items()).isEmpty();
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void a_page_returns_its_slice_and_reports_whether_a_later_page_exists() {
		List<Auction> five = List.of(
				open(NOW.plus(Duration.ofHours(1))),
				open(NOW.plus(Duration.ofHours(2))),
				open(NOW.plus(Duration.ofHours(3))),
				open(NOW.plus(Duration.ofHours(4))),
				open(NOW.plus(Duration.ofHours(5))));

		AuctionPage<Auction> second = auctions.findOpen(NOW, new PageQuery(1, 2));
		AuctionPage<Auction> last = auctions.findOpen(NOW, new PageQuery(2, 2));

		assertThat(ids(second)).containsExactly(five.get(2).id(), five.get(3).id());
		assertThat(second.hasNext()).isTrue();
		assertThat(ids(last)).containsExactly(five.get(4).id());
		assertThat(last.hasNext()).isFalse();
	}

	private Auction open(Instant endsAt) {
		return auctions.save(Auction.open(new UserId(1), CAR, new BigDecimal("12000.00"),
				new BigDecimal("100.00"), endsAt, CREATED_AT, WINDOW));
	}

	private static List<Long> ids(AuctionPage<Auction> page) {
		return page.items().stream().map(Auction::id).toList();
	}

}
