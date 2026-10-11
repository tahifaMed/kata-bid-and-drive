package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * An auction for one car. {@code id} is null until the auction is saved.
 * {@code leadingAmount} and {@code leadingBidderId} are null while there are no bids.
 * {@code version} is the stored version that storage checks when a bid is recorded (ADR 0003).
 */
public record Auction(
		Long id,
		UserId sellerId,
		Car car,
		Money startingPrice,
		Money minIncrement,
		Instant endsAt,
		AuctionStatus status,
		Money leadingAmount,
		UserId leadingBidderId,
		int bidCount,
		long version) {

	public static final int FIRST_YEAR = 1886;

	/** An auction with no bids yet. */
	public Auction(Long id, UserId sellerId, Car car, Money startingPrice, Money minIncrement, Instant endsAt,
			AuctionStatus status) {
		this(id, sellerId, car, startingPrice, minIncrement, endsAt, status, null, null, 0, 0);
	}

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

	/** The highest bid, or the starting price while there are no bids. */
	public Money currentBid() {
		return leadingAmount == null ? startingPrice : leadingAmount;
	}

	/** The starting price while there are no bids, otherwise the highest bid plus the increment (ADR 0006). */
	public Money minNextBid() {
		return leadingAmount == null
				? startingPrice
				: new Money(leadingAmount.amount().add(minIncrement.amount()));
	}

	/** Empty while there are no bids. */
	public Optional<UserId> highestBidderId() {
		return Optional.ofNullable(leadingBidderId);
	}

	/**
	 * Accepts a bid, or throws the first rule it breaks, in the order of ADR 0005:
	 * seller, closed, already highest, minimum. The version is kept for storage to check.
	 */
	public BidPlacement placeBid(UserId bidder, Money amount, Instant now) {
		if (sellerId.equals(bidder)) {
			throw new SellerCannotBidException();
		}
		if (!isBiddableAt(now)) {
			throw new AuctionClosedException();
		}
		if (bidder.equals(leadingBidderId)) {
			throw new AlreadyHighestBidderException();
		}
		Money minimum = minNextBid();
		if (amount.amount().compareTo(minimum.amount()) < 0) {
			throw new BidBelowMinimumException(minimum);
		}
		Auction updated = new Auction(id, sellerId, car, startingPrice, minIncrement, endsAt, status,
				amount, bidder, bidCount + 1, version);
		return new BidPlacement(updated, new Bid(null, id, bidder, amount, now));
	}

	/** Closed from {@code endsAt} on. The stored status stays OPEN; the phase is derived on read (ADR 0016). */
	public AuctionPhase phaseAt(Instant now) {
		return endsAt.isAfter(now) ? AuctionPhase.OPEN : AuctionPhase.CLOSED;
	}

	public boolean isBiddableAt(Instant now) {
		return phaseAt(now) == AuctionPhase.OPEN;
	}

	/** Empty while open. Once closed, SOLD to the highest bidder at {@code currentBid}, or NO_BIDS. */
	public Optional<AuctionOutcome> outcomeAt(Instant now) {
		if (phaseAt(now) == AuctionPhase.OPEN) {
			return Optional.empty();
		}
		return Optional.of(bidCount == 0 ? AuctionOutcome.NO_BIDS : AuctionOutcome.SOLD);
	}

	private static void checkYear(int year, Instant now) {
		int latest = Year.from(now.atZone(ZoneOffset.UTC)).getValue() + 1;
		if (year < FIRST_YEAR || year > latest) {
			throw new InvalidAuctionException("car.year", "must be between " + FIRST_YEAR + " and " + latest);
		}
	}

}
