package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionBiddingTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2100-01-01T12:00:00Z"), ZoneOffset.UTC);
	private static final Instant NOW = CLOCK.instant();
	private static final UserId SELLER = new UserId(1);
	private static final UserId BOB = new UserId(2);
	private static final UserId CHLOE = new UserId(3);

	@Test
	void the_first_bid_may_equal_the_starting_price() {
		BidPlacement placement = open().placeBid(BOB, money("1000.00"), NOW);

		assertThat(placement.bid().id()).isNull();
		assertThat(placement.bid().auctionId()).isEqualTo(5L);
		assertThat(placement.bid().bidderId()).isEqualTo(BOB);
		assertThat(placement.bid().amount()).isEqualTo(money("1000.00"));
		assertThat(placement.bid().placedAt()).isEqualTo(NOW);
	}

	@Test
	void a_placed_bid_becomes_the_current_bid_and_raises_the_minimum() {
		Auction auction = open().placeBid(BOB, money("1000.00"), NOW).auction();

		assertThat(auction.currentBid()).isEqualTo(money("1000.00"));
		assertThat(auction.minNextBid()).isEqualTo(money("1100.00"));
		assertThat(auction.bidCount()).isEqualTo(1);
		assertThat(auction.highestBidderId()).contains(BOB);
	}

	@Test
	void placing_a_bid_keeps_the_version_so_storage_can_check_it() {
		Auction auction = withBid(BOB, "1000.00", 7).placeBid(CHLOE, money("1100.00"), NOW).auction();

		assertThat(auction.version()).isEqualTo(7);
		assertThat(auction.bidCount()).isEqualTo(2);
	}

	@Test
	void the_first_bid_below_the_starting_price_is_rejected_with_the_starting_price_as_minimum() {
		assertThatThrownBy(() -> open().placeBid(BOB, money("999.99"), NOW))
				.isInstanceOfSatisfying(BidBelowMinimumException.class,
						e -> assertThat(e.minimumBid()).isEqualTo(money("1000.00")));
	}

	@Test
	void a_bid_below_current_bid_plus_increment_is_rejected_with_the_minimum() {
		Auction auction = withBid(BOB, "1000.00", 1);

		assertThatThrownBy(() -> auction.placeBid(CHLOE, money("1050.00"), NOW))
				.isInstanceOfSatisfying(BidBelowMinimumException.class,
						e -> assertThat(e.minimumBid()).isEqualTo(money("1100.00")));
		assertThat(auction.placeBid(CHLOE, money("1100.00"), NOW).auction().currentBid())
				.isEqualTo(money("1100.00"));
	}

	@Test
	void the_seller_cannot_bid_on_their_own_auction() {
		assertThatThrownBy(() -> open().placeBid(SELLER, money("1000.00"), NOW))
				.isInstanceOf(SellerCannotBidException.class);
	}

	@Test
	void the_highest_bidder_cannot_bid_again() {
		assertThatThrownBy(() -> withBid(BOB, "1000.00", 1).placeBid(BOB, money("2000.00"), NOW))
				.isInstanceOf(AlreadyHighestBidderException.class);
	}

	@Test
	void an_auction_is_closed_at_its_end_time() {
		Auction auction = auctionEndingAt(NOW);

		assertThatThrownBy(() -> auction.placeBid(CHLOE, money("1000.00"), NOW))
				.isInstanceOf(AuctionClosedException.class);
	}

	@Test
	void closed_wins_over_a_bid_below_the_minimum() {
		Auction auction = auctionEndingAt(NOW.minusSeconds(1));

		assertThatThrownBy(() -> auction.placeBid(CHLOE, money("1.00"), NOW))
				.isInstanceOf(AuctionClosedException.class);
	}

	@Test
	void the_seller_check_runs_before_the_closed_check() {
		Auction auction = auctionEndingAt(NOW);

		assertThatThrownBy(() -> auction.placeBid(SELLER, money("1000.00"), NOW))
				.isInstanceOf(SellerCannotBidException.class);
	}

	@Test
	void already_highest_wins_over_a_bid_below_the_minimum() {
		assertThatThrownBy(() -> withBid(BOB, "1000.00", 1).placeBid(BOB, money("1.00"), NOW))
				.isInstanceOf(AlreadyHighestBidderException.class);
	}

	@Test
	void every_rejection_is_a_bid_rejection() {
		assertThatThrownBy(() -> open().placeBid(SELLER, money("1000.00"), NOW))
				.isInstanceOf(BidRejectedException.class);
	}

	private static Auction open() {
		return auctionEndingAt(NOW.plusSeconds(3600));
	}

	private static Auction auctionEndingAt(Instant endsAt) {
		return new Auction(5L, SELLER, car(), money("1000.00"), money("100.00"), endsAt, AuctionStatus.OPEN);
	}

	private static Auction withBid(UserId bidder, String amount, long version) {
		return new Auction(5L, SELLER, car(), money("1000.00"), money("100.00"), NOW.plusSeconds(3600),
				AuctionStatus.OPEN, money(amount), bidder, 1, version);
	}

	private static Car car() {
		return new Car("Volkswagen", "Golf", 2020, 42000, null, null);
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

}
