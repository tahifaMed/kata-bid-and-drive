package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BidderStandingTests {

	private static final Instant ENDS_AT = Instant.parse("2026-10-12T18:00:00Z");
	private static final Instant BEFORE_END = ENDS_AT.minusMillis(1);
	private static final UserId BOB = new UserId(2);
	private static final UserId DAN = new UserId(4);

	@Test
	void the_highest_bidder_is_winning() {
		BidderStanding standing = new BidderStanding(auctionLedBy(BOB, "1200.00"), BOB, money("1200.00"));

		assertThat(standing.status(BEFORE_END)).isEqualTo(BidderStatus.WINNING);
	}

	@Test
	void a_bidder_who_is_no_longer_highest_is_outbid() {
		BidderStanding standing = new BidderStanding(auctionLedBy(DAN, "1000.00"), BOB, money("900.00"));

		assertThat(standing.status(BEFORE_END)).isEqualTo(BidderStatus.OUTBID);
	}

	@Test
	void the_highest_bidder_has_won_once_the_auction_ends() {
		BidderStanding standing = new BidderStanding(auctionLedBy(BOB, "1200.00"), BOB, money("1200.00"));

		assertThat(standing.status(ENDS_AT)).isEqualTo(BidderStatus.WON);
	}

	@Test
	void an_outbid_bidder_has_lost_once_the_auction_ends() {
		BidderStanding standing = new BidderStanding(auctionLedBy(DAN, "1000.00"), BOB, money("900.00"));

		assertThat(standing.status(ENDS_AT)).isEqualTo(BidderStatus.LOST);
	}

	private static Auction auctionLedBy(UserId leader, String amount) {
		return new Auction(5L, new UserId(1), new Car("Fiat", "Panda", 2015, 90000, null, null),
				money("800.00"), money("100.00"), ENDS_AT, AuctionStatus.OPEN,
				money(amount), leader, 2, 2);
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

}
