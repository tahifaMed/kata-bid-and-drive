package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionPricesTests {

	private static final Instant ENDS_AT = Instant.parse("2100-01-02T00:00:00Z");

	@Test
	void with_no_bids_the_current_bid_and_the_minimum_next_bid_are_the_starting_price() {
		Auction auction = auction();

		assertThat(auction.currentBid().amount()).isEqualByComparingTo("12000.00");
		assertThat(auction.minNextBid().amount()).isEqualByComparingTo("12000.00");
	}

	@Test
	void with_no_bids_the_count_is_zero_and_there_is_no_highest_bidder() {
		Auction auction = auction();

		assertThat(auction.bidCount()).isZero();
		assertThat(auction.highestBidderId()).isEmpty();
	}

	@Test
	void an_open_auction_is_biddable_until_one_second_before_it_ends() {
		assertThat(auction().isBiddableAt(ENDS_AT.minusSeconds(1))).isTrue();
	}

	@Test
	void an_open_auction_is_not_biddable_at_or_after_its_end() {
		assertThat(auction().isBiddableAt(ENDS_AT)).isFalse();
		assertThat(auction().isBiddableAt(ENDS_AT.plusSeconds(1))).isFalse();
	}

	private static Auction auction() {
		return new Auction(1L, new UserId(1),
				new Car("Volkswagen", "Golf", 2020, 42000, null, null),
				new Money(new BigDecimal("12000.00")), new Money(new BigDecimal("100.00")),
				ENDS_AT, AuctionStatus.OPEN);
	}

}
