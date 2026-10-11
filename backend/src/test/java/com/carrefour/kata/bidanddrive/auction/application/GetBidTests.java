package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetBidTests {

	private static final UserId BOB = new UserId(2);
	private static final Bid BOBS_BID = new Bid(12L, 5, BOB, new Money(new BigDecimal("1000.00")),
			Instant.parse("2026-10-11T10:00:00Z"));

	private final GetBid getBid = new GetBid(new BidRepository() {
		@Override
		public List<Bid> findNewest(long auctionId, int limit) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public Optional<Bid> findById(long auctionId, long bidId) {
			return auctionId == 5 && bidId == 12 ? Optional.of(BOBS_BID) : Optional.empty();
		}

		@Override
		public List<BidderStanding> findStandings(UserId bidder) {
			throw new UnsupportedOperationException("not used by this test");
		}
	});

	@Test
	void the_bidder_reads_their_own_bid() {
		assertThat(getBid.execute(5, 12, BOB)).isEqualTo(BOBS_BID);
	}

	@Test
	void another_user_cannot_tell_the_bid_exists() {
		assertThatThrownBy(() -> getBid.execute(5, 12, new UserId(3)))
				.isInstanceOf(BidNotFoundException.class)
				.hasMessage("Bid 12 does not exist in auction 5");
	}

	@Test
	void a_bid_outside_the_auction_is_not_found() {
		assertThatThrownBy(() -> getBid.execute(6, 12, BOB)).isInstanceOf(BidNotFoundException.class);
	}

}
