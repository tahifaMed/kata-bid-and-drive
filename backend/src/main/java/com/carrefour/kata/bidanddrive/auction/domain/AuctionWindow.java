package com.carrefour.kata.bidanddrive.auction.domain;

import java.time.Duration;
import java.time.Instant;

/** How far after "now" an auction may end. Both bounds are inclusive. See ADR 0008. */
public record AuctionWindow(Duration minimum, Duration maximum) {

	public void check(Instant endsAt, Instant now) {
		if (endsAt.isBefore(now.plus(minimum))) {
			throw new InvalidAuctionException("endsAt", "is too soon");
		}
		if (endsAt.isAfter(now.plus(maximum))) {
			throw new InvalidAuctionException("endsAt", "is too late");
		}
	}

}
