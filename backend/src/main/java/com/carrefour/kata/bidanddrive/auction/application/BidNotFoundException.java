package com.carrefour.kata.bidanddrive.auction.application;

/** The bid does not exist in that auction, or the caller may not see it (ADR 0013). The message is the same. */
public class BidNotFoundException extends RuntimeException {

	public BidNotFoundException(long auctionId, long bidId) {
		super("Bid " + bidId + " does not exist in auction " + auctionId);
	}

}
