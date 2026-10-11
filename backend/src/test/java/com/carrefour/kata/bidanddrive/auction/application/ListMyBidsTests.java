package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
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
			return bidder.equals(BOB) ? List.of(BOBS_STANDING) : List.of();
		}
	});

	@Test
	void the_caller_gets_their_standing_on_each_auction_they_bid_on() {
		assertThat(listMyBids.execute(BOB)).containsExactly(BOBS_STANDING);
	}

	@Test
	void a_caller_who_never_bid_gets_an_empty_list() {
		assertThat(listMyBids.execute(new UserId(3))).isEmpty();
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

}
