package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionPricesTests {

	@Test
	void with_no_bids_the_current_bid_and_the_minimum_next_bid_are_the_starting_price() {
		Auction auction = new Auction(1L, new UserId(1),
				new Car("Volkswagen", "Golf", 2020, 42000, null, null),
				new Money(new BigDecimal("12000.00")), new Money(new BigDecimal("100.00")),
				Instant.parse("2100-01-02T00:00:00Z"), AuctionStatus.OPEN);

		assertThat(auction.currentBid().amount()).isEqualByComparingTo("12000.00");
		assertThat(auction.minNextBid().amount()).isEqualByComparingTo("12000.00");
	}

}
