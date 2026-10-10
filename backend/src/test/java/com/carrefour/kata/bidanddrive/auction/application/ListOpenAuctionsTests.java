package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListOpenAuctionsTests {

	private static final Instant NOW = Instant.parse("2100-01-01T00:00:00Z");

	@Test
	void asks_the_repository_for_open_auctions_at_the_clock_now_with_the_requested_page() {
		RecordingAuctions auctions = new RecordingAuctions();
		ListOpenAuctions listOpenAuctions = new ListOpenAuctions(auctions, Clock.fixed(NOW, ZoneOffset.UTC));

		listOpenAuctions.execute(new PageQuery(1, 2));

		assertThat(auctions.now).isEqualTo(NOW);
		assertThat(auctions.query).isEqualTo(new PageQuery(1, 2));
	}

	@Test
	void returns_the_page_the_repository_gives() {
		AuctionPage<Auction> expected = new AuctionPage<>(List.of(), 0, 20, false);
		RecordingAuctions auctions = new RecordingAuctions(expected);
		ListOpenAuctions listOpenAuctions = new ListOpenAuctions(auctions, Clock.fixed(NOW, ZoneOffset.UTC));

		AuctionPage<Auction> page = listOpenAuctions.execute(new PageQuery(0, 20));

		assertThat(page).isSameAs(expected);
	}

	/** Hand-written fake: records the arguments of {@link AuctionRepository#findOpen}. */
	private static class RecordingAuctions implements AuctionRepository {

		private final AuctionPage<Auction> result;
		Instant now;
		PageQuery query;

		RecordingAuctions() {
			this(new AuctionPage<>(List.of(), 0, 20, false));
		}

		RecordingAuctions(AuctionPage<Auction> result) {
			this.result = result;
		}

		@Override
		public Auction save(Auction auction) {
			throw new UnsupportedOperationException("not used by this test");
		}

		@Override
		public AuctionPage<Auction> findOpen(Instant now, PageQuery query) {
			this.now = now;
			this.query = query;
			return result;
		}

	}

}
