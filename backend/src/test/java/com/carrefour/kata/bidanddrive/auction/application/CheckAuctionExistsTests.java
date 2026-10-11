package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckAuctionExistsTests {

	private final CheckAuctionExists check = new CheckAuctionExists(new FakeAuctions(Set.of(5L)));

	@Test
	void an_existing_auction_passes() {
		assertThatNoException().isThrownBy(() -> check.execute(5));
	}

	@Test
	void an_unknown_auction_is_not_found() {
		assertThatThrownBy(() -> check.execute(6))
				.isInstanceOf(AuctionNotFoundException.class)
				.hasMessage("Auction 6 does not exist");
	}

	private record FakeAuctions(Set<Long> ids) implements AuctionRepository {

		@Override
		public boolean existsById(long id) {
			return ids.contains(id);
		}

		@Override
		public Auction save(Auction auction) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public Bid recordBid(Auction auction, Bid bid) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public AuctionPage<Auction> findOpen(Instant now, PageQuery query) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public Optional<Auction> findById(long id) {
			throw new UnsupportedOperationException("not used by this test");
		}

	}

}
