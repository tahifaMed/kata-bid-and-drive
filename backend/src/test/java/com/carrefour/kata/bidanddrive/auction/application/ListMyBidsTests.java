package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ListMyBidsTests {

	private static final UserId BOB = new UserId(2);
	private static final BidderStanding BOBS_STANDING = new BidderStanding(
			new Auction(5L, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
					money("1000.00"), money("100.00"), Instant.parse("2026-10-12T18:00:00Z"), AuctionStatus.OPEN,
					money("1200.00"), BOB, 1, 1),
			BOB, money("1200.00"));
	private static final BidderStanding DANS_LEAD = new BidderStanding(
			new Auction(6L, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
					money("1000.00"), money("100.00"), Instant.parse("2026-10-12T18:00:00Z"), AuctionStatus.OPEN,
					money("1100.00"), new UserId(4), 2, 2),
			BOB, money("1000.00"));

	private static final Instant ENDS_AT = Instant.parse("2026-10-12T18:00:00Z");
	private final MutableClock clock = new MutableClock(ENDS_AT.minusMillis(1));
	private final ListMyBids listMyBids = new ListMyBids(new BidRepository() {
		@Override
		public List<Bid> findNewest(long auctionId, int limit) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public Optional<Bid> findById(long auctionId, long bidId) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public Optional<BidderStanding> findStanding(long auctionId, UserId bidder) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public List<BidderStanding> findStandings(UserId bidder) {
			return bidder.equals(BOB) ? List.of(BOBS_STANDING, DANS_LEAD) : List.of();
		}
	}, clock);

	@Test
	void the_caller_gets_their_standing_on_each_auction_they_bid_on() {
		assertThat(listMyBids.execute(BOB)).containsExactly(
				new MyBidView(BOBS_STANDING, BidderStatus.WINNING),
				new MyBidView(DANS_LEAD, BidderStatus.OUTBID));
	}

	@Test
	void once_the_auctions_end_the_caller_has_won_or_lost() {
		clock.set(ENDS_AT);

		assertThat(listMyBids.execute(BOB)).extracting(MyBidView::status)
				.containsExactly(BidderStatus.WON, BidderStatus.LOST);
	}

	@Test
	void a_caller_who_never_bid_gets_an_empty_list() {
		assertThat(listMyBids.execute(new UserId(3))).isEmpty();
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void set(Instant now) {
			this.now = now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Instant instant() {
			return now;
		}
	}

}
