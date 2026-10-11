package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.time.Instant;

/**
 * A bidder's position on one auction they bid on. {@code myHighestBid} is the largest of their bids there.
 * The status is derived from the auction's highest bidder, never stored, so it cannot drift from the bids.
 */
public record BidderStanding(Auction auction, UserId bidder, Money myHighestBid) {

	/** Bids are strictly increasing, so there are no ties. */
	public BidderStatus status(Instant now) {
		boolean highest = auction.highestBidderId().filter(bidder::equals).isPresent();
		return switch (auction.phaseAt(now)) {
			case OPEN -> highest ? BidderStatus.WINNING : BidderStatus.OUTBID;
			case CLOSED -> highest ? BidderStatus.WON : BidderStatus.LOST;
		};
	}

}
