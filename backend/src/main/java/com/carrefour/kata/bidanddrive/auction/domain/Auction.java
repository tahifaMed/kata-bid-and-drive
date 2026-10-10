package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;

/** An auction for one car. {@code id} is null until the auction is saved. */
public record Auction(
		Long id,
		UserId sellerId,
		Car car,
		Money startingPrice,
		Money minIncrement,
		Instant endsAt,
		AuctionStatus status) {

	public static final int FIRST_YEAR = 1886;

	public static Auction open(
			UserId sellerId,
			Car car,
			BigDecimal startingPrice,
			BigDecimal minIncrement,
			Instant endsAt,
			Instant now,
			AuctionWindow window) {
		Money start = Money.positive("startingPrice", startingPrice);
		Money increment = Money.positive("minIncrement", minIncrement);
		checkYear(car.year(), now);
		if (endsAt == null) {
			throw new InvalidAuctionException("endsAt", "is required");
		}
		window.check(endsAt, now);
		return new Auction(null, sellerId, car, start, increment, endsAt, AuctionStatus.OPEN);
	}

	/** The highest bid, or the starting price while there are no bids. Spec 007 adds bids to this rule. */
	public Money currentBid() {
		return startingPrice;
	}

	/** The starting price while there are no bids (spec 004). */
	public Money minNextBid() {
		return startingPrice;
	}

	private static void checkYear(int year, Instant now) {
		int latest = Year.from(now.atZone(ZoneOffset.UTC)).getValue() + 1;
		if (year < FIRST_YEAR || year > latest) {
			throw new InvalidAuctionException("car.year", "must be between " + FIRST_YEAR + " and " + latest);
		}
	}

}
