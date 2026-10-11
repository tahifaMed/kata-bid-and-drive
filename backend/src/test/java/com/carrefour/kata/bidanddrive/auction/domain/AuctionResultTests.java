package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionResultTests {

	private static final Instant ENDS_AT = Instant.parse("2100-01-01T12:00:00Z");
	private static final UserId BOB = new UserId(3);

	@Test
	void an_auction_is_open_before_its_end_time() {
		Auction auction = noBids();

		assertThat(auction.phaseAt(ENDS_AT.minusMillis(1))).isEqualTo(AuctionPhase.OPEN);
		assertThat(auction.isBiddableAt(ENDS_AT.minusMillis(1))).isTrue();
	}

	@Test
	void an_auction_is_closed_at_its_end_time() {
		Auction auction = noBids();

		assertThat(auction.phaseAt(ENDS_AT)).isEqualTo(AuctionPhase.CLOSED);
		assertThat(auction.isBiddableAt(ENDS_AT)).isFalse();
	}

	@Test
	void an_open_auction_has_no_outcome() {
		assertThat(ledBy(BOB, "5200.00").outcomeAt(ENDS_AT.minusMillis(1))).isEmpty();
	}

	@Test
	void a_closed_auction_with_bids_is_sold() {
		assertThat(ledBy(BOB, "5200.00").outcomeAt(ENDS_AT)).contains(AuctionOutcome.SOLD);
	}

	@Test
	void a_closed_auction_without_bids_has_no_bids_outcome() {
		assertThat(noBids().outcomeAt(ENDS_AT.plusSeconds(60))).contains(AuctionOutcome.NO_BIDS);
	}

	private static Auction noBids() {
		return new Auction(7L, new UserId(1), car(), money("1000.00"), money("100.00"), ENDS_AT, AuctionStatus.OPEN);
	}

	private static Auction ledBy(UserId leader, String amount) {
		return new Auction(6L, new UserId(1), car(), money("1000.00"), money("100.00"), ENDS_AT, AuctionStatus.OPEN,
				money(amount), leader, 3, 3);
	}

	private static Car car() {
		return new Car("Fiat", "Panda", 2015, 90000, null, null);
	}

	private static Money money(String amount) {
		return new Money(new BigDecimal(amount));
	}

}
