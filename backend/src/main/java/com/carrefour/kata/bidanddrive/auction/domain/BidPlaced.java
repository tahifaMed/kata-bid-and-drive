package com.carrefour.kata.bidanddrive.auction.domain;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.time.Instant;

/**
 * A bid was placed. Carries the auction state after the bid, so a client can render it without a second call.
 * {@code bidderName} is already masked (006). {@code extended} is always false until spec 012.
 */
public record BidPlaced(
		long auctionId,
		Instant occurredAt,
		long bidId,
		Money amount,
		UserId bidderId,
		String bidderName,
		UserId highestBidderId,
		Money currentBid,
		Money minNextBid,
		int bidCount,
		Instant endsAt,
		boolean extended) implements AuctionEvent {

	/** Builds the event from the auction after the bid and the saved bid, which has its id. */
	public static BidPlaced of(Auction afterBid, Bid saved, String bidderDisplayName) {
		return new BidPlaced(
				afterBid.id(),
				saved.placedAt(),
				saved.id(),
				saved.amount(),
				saved.bidderId(),
				BidderName.mask(bidderDisplayName),
				afterBid.leadingBidderId(),
				afterBid.currentBid(),
				afterBid.minNextBid(),
				afterBid.bidCount(),
				afterBid.endsAt(),
				false);
	}

}
