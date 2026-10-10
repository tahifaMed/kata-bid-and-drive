package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidPlacement;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BidPersistenceTests {

	private static final UserId BOB = new UserId(2);
	private static final UserId CHLOE = new UserId(3);

	// TIMESTAMPTZ stores microseconds; Linux clocks can give nanoseconds.
	private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

	private final JpaAuctionRepository auctions;
	private final JpaBidRepository bids;
	private final JdbcTemplate jdbc;

	@Autowired
	BidPersistenceTests(JpaAuctionRepository auctions, JpaBidRepository bids, JdbcTemplate jdbc) {
		this.auctions = auctions;
		this.bids = bids;
		this.jdbc = jdbc;
	}

	@Test
	void a_new_auction_has_no_bids_and_version_zero() {
		Auction found = auctions.findById(newAuction().id()).orElseThrow();

		assertThat(found.bidCount()).isZero();
		assertThat(found.highestBidderId()).isEmpty();
		assertThat(found.currentBid().amount()).isEqualByComparingTo("1000.00");
		assertThat(found.version()).isZero();
	}

	@Test
	void recording_a_bid_stores_the_bid_row_and_the_auction_totals() {
		Auction auction = newAuction();
		BidPlacement placement = auction.placeBid(BOB, money("1000.00"), now);

		Bid saved = auctions.recordBid(placement.auction(), placement.bid());

		assertThat(saved.id()).isNotNull();
		assertThat(saved).isEqualTo(new Bid(saved.id(), auction.id(), BOB, money("1000.00"), now));
		Auction found = auctions.findById(auction.id()).orElseThrow();
		assertThat(found.currentBid()).isEqualTo(money("1000.00"));
		assertThat(found.minNextBid()).isEqualTo(money("1100.00"));
		assertThat(found.bidCount()).isEqualTo(1);
		assertThat(found.highestBidderId()).contains(BOB);
		assertThat(found.version()).isEqualTo(1);
		assertThat(bids.findById(auction.id(), saved.id())).contains(saved);
	}

	@Test
	void recording_a_bid_with_a_stale_version_is_a_conflict_and_stores_nothing() {
		Auction loaded = newAuction();
		BidPlacement first = loaded.placeBid(BOB, money("1000.00"), now);
		auctions.recordBid(first.auction(), first.bid());
		BidPlacement stale = loaded.placeBid(CHLOE, money("1000.00"), now);

		assertThatThrownBy(() -> auctions.recordBid(stale.auction(), stale.bid()))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		Auction found = auctions.findById(loaded.id()).orElseThrow();
		assertThat(found.bidCount()).isEqualTo(1);
		assertThat(found.highestBidderId()).contains(BOB);
		assertThat(countBids(loaded.id())).isEqualTo(1);
	}

	@Test
	void newest_bids_come_first_and_are_capped_at_the_limit() {
		Auction auction = newAuction();
		Bid first = record(auctions.findById(auction.id()).orElseThrow(), BOB, "1000.00", now.minusSeconds(20));
		Bid second = record(auctions.findById(auction.id()).orElseThrow(), CHLOE, "1100.00", now.minusSeconds(10));
		Bid third = record(auctions.findById(auction.id()).orElseThrow(), BOB, "1200.00", now.minusSeconds(10));

		assertThat(bids.findNewest(auction.id(), 2)).containsExactly(third, second);
		assertThat(bids.findNewest(auction.id(), 50)).containsExactly(third, second, first);
	}

	@Test
	void a_bid_is_found_only_under_its_own_auction() {
		Auction auction = newAuction();
		Bid saved = record(auction, BOB, "1000.00", now);

		assertThat(bids.findById(newAuction().id(), saved.id())).isEmpty();
		assertThat(bids.findById(auction.id(), Long.MAX_VALUE)).isEmpty();
	}

	private Bid record(Auction auction, UserId bidder, String amount, Instant placedAt) {
		BidPlacement placement = auction.placeBid(bidder, money(amount), placedAt);
		return auctions.recordBid(placement.auction(), placement.bid());
	}

	private Auction newAuction() {
		return auctions.save(new Auction(null, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
				money("1000.00"), money("100.00"), now.plus(Duration.ofDays(1)), AuctionStatus.OPEN));
	}

	private int countBids(long auctionId) {
		return jdbc.queryForObject("SELECT count(*) FROM bid WHERE auction_id = ?", Integer.class, auctionId);
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

}
