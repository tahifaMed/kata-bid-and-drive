package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BidPlacedTests {

	private static final Instant NOW = Instant.parse("2100-01-01T12:00:00Z");
	private static final Instant ENDS_AT = Instant.parse("2100-01-02T12:00:00Z");
	private static final UserId SELLER = new UserId(1);
	private static final UserId BOB = new UserId(2);

	@Test
	void carries_the_auction_state_after_the_bid() {
		BidPlacement placement = withOneBid().placeBid(BOB, money("1200.00"), NOW);
		Bid saved = new Bid(42L, 5L, BOB, money("1200.00"), NOW);

		BidPlaced event = BidPlaced.of(placement.auction(), saved, "Bob");

		assertThat(event.auctionId()).isEqualTo(5L);
		assertThat(event.occurredAt()).isEqualTo(NOW);
		assertThat(event.bidId()).isEqualTo(42L);
		assertThat(event.amount()).isEqualTo(money("1200.00"));
		assertThat(event.bidderId()).isEqualTo(BOB);
		assertThat(event.highestBidderId()).isEqualTo(BOB);
		assertThat(event.currentBid()).isEqualTo(money("1200.00"));
		assertThat(event.minNextBid()).isEqualTo(money("1300.00"));
		assertThat(event.bidCount()).isEqualTo(2);
		assertThat(event.endsAt()).isEqualTo(ENDS_AT);
		assertThat(event.extended()).isFalse();
	}

	@Test
	void masks_the_bidder_name() {
		BidPlacement placement = withOneBid().placeBid(BOB, money("1200.00"), NOW);
		Bid saved = new Bid(42L, 5L, BOB, money("1200.00"), NOW);

		assertThat(BidPlaced.of(placement.auction(), saved, "Bob").bidderName()).isEqualTo("B***");
	}

	private static Auction withOneBid() {
		return new Auction(5L, SELLER, new Car("Volkswagen", "Golf", 2020, 42000, null, null), money("1000.00"),
				money("100.00"), ENDS_AT, AuctionStatus.OPEN, money("1000.00"), new UserId(3), 1, 0);
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

}
