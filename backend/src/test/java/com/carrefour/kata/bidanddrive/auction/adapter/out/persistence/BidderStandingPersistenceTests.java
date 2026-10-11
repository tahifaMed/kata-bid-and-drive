package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.BidPlacement;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** The database is shared between tests, so each test looks only at the auctions it made. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BidderStandingPersistenceTests {

	private static final UserId BOB = new UserId(2);
	private static final UserId CHLOE = new UserId(3);
	private static final UserId DAN = new UserId(4);

	// TIMESTAMPTZ stores microseconds; Linux clocks can give nanoseconds.
	private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

	private final JpaAuctionRepository auctions;
	private final JpaBidRepository bids;

	@Autowired
	BidderStandingPersistenceTests(JpaAuctionRepository auctions, JpaBidRepository bids) {
		this.auctions = auctions;
		this.bids = bids;
	}

	@Test
	void each_auction_comes_with_the_bidders_highest_bid_and_status() {
		long winning = newAuction(now.plus(Duration.ofDays(2)));
		long outbid = newAuction(now.plus(Duration.ofDays(1)));
		record(winning, BOB, "1200.00");
		record(outbid, BOB, "900.00");
		record(outbid, DAN, "1000.00");

		List<BidderStanding> standings = standingsOn(BOB, winning, outbid);

		assertThat(standings).extracting(standing -> standing.auction().id()).containsExactly(outbid, winning);
		assertThat(standings).extracting(BidderStanding::myHighestBid)
				.containsExactly(money("900.00"), money("1200.00"));
		assertThat(standings).extracting(BidderStanding::status)
				.containsExactly(BidderStatus.OUTBID, BidderStatus.WINNING);
		assertThat(standings.getFirst().auction().currentBid()).isEqualTo(money("1000.00"));
		assertThat(standings.getFirst().auction().car().make()).isEqualTo("Fiat");
	}

	@Test
	void several_bids_on_one_auction_give_one_item_with_the_highest() {
		long id = newAuction(now.plus(Duration.ofDays(1)));
		record(id, BOB, "1000.00");
		record(id, DAN, "1100.00");
		record(id, BOB, "1200.00");

		List<BidderStanding> standings = standingsOn(BOB, id);

		assertThat(standings).hasSize(1);
		assertThat(standings.getFirst().myHighestBid()).isEqualTo(money("1200.00"));
		assertThat(standings.getFirst().status()).isEqualTo(BidderStatus.WINNING);
	}

	@Test
	void auctions_ending_together_are_ordered_by_id() {
		Instant endsAt = now.plus(Duration.ofDays(1));
		long first = newAuction(endsAt);
		long second = newAuction(endsAt);
		record(second, BOB, "1000.00");
		record(first, BOB, "1000.00");

		assertThat(standingsOn(BOB, first, second)).extracting(standing -> standing.auction().id())
				.containsExactly(first, second);
	}

	@Test
	void an_ended_auction_is_still_listed() {
		Instant placedAt = now.minus(Duration.ofDays(2));
		long ended = newAuction(now.minus(Duration.ofDays(1)));
		record(ended, BOB, "1000.00", placedAt);

		assertThat(standingsOn(BOB, ended)).hasSize(1);
	}

	@Test
	void auctions_the_bidder_did_not_bid_on_are_left_out() {
		long id = newAuction(now.plus(Duration.ofDays(1)));
		record(id, CHLOE, "1000.00");

		assertThat(standingsOn(BOB, id)).isEmpty();
	}

	@Test
	void one_auction_gives_the_bidders_standing_there_or_nothing() {
		long id = newAuction(now.plus(Duration.ofDays(1)));
		record(id, BOB, "1000.00");
		record(id, DAN, "1100.00");

		BidderStanding bobs = bids.findStanding(id, BOB).orElseThrow();
		assertThat(bobs.myHighestBid()).isEqualTo(money("1000.00"));
		assertThat(bobs.status()).isEqualTo(BidderStatus.OUTBID);
		assertThat(bids.findStanding(id, DAN).orElseThrow().status()).isEqualTo(BidderStatus.WINNING);
		assertThat(bids.findStanding(id, CHLOE)).isEmpty();
		assertThat(bids.findStanding(newAuction(now.plus(Duration.ofDays(1))), BOB)).isEmpty();
	}

	private List<BidderStanding> standingsOn(UserId bidder, Long... auctionIds) {
		Set<Long> ids = Set.of(auctionIds);
		return bids.findStandings(bidder).stream()
				.filter(standing -> ids.contains(standing.auction().id()))
				.toList();
	}

	private void record(long auctionId, UserId bidder, String amount) {
		record(auctionId, bidder, amount, now);
	}

	private void record(long auctionId, UserId bidder, String amount, Instant placedAt) {
		BidPlacement placement = auctions.findById(auctionId).orElseThrow()
				.placeBid(bidder, money(amount), placedAt);
		auctions.recordBid(placement.auction(), placement.bid());
	}

	private long newAuction(Instant endsAt) {
		return auctions.save(new Auction(null, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
				money("800.00"), money("100.00"), endsAt, AuctionStatus.OPEN)).id();
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

}
