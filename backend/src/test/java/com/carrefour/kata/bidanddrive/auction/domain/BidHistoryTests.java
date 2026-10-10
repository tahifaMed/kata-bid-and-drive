package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class BidHistoryTests {

	private static final Instant START = Instant.parse("2100-01-01T00:00:00Z");

	@Test
	void keeps_the_fifty_newest_bids_newest_first() {
		List<Bid> bids = LongStream.rangeClosed(1, 51)
				.mapToObj(id -> bid(id, START.plusSeconds(id)))
				.toList();

		List<Bid> newest = BidHistory.newest(bids);

		assertThat(newest).hasSize(50);
		assertThat(newest.getFirst().id()).isEqualTo(51);
		assertThat(newest.getLast().id()).isEqualTo(2);
	}

	@Test
	void bids_placed_at_the_same_instant_are_ordered_by_id_descending() {
		List<Bid> newest = BidHistory.newest(List.of(bid(7, START), bid(9, START), bid(8, START)));

		assertThat(newest).extracting(Bid::id).containsExactly(9L, 8L, 7L);
	}

	@Test
	void no_bids_give_an_empty_history() {
		assertThat(BidHistory.newest(List.of())).isEmpty();
	}

	private static Bid bid(long id, Instant placedAt) {
		return new Bid(id, new UserId(2), new Money(new BigDecimal("1000.00")), placedAt);
	}

}
