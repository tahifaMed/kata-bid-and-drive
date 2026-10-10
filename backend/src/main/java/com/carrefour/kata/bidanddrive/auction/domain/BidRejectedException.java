package com.carrefour.kata.bidanddrive.auction.domain;

/** A bid broke a bidding rule (ADR 0006). The web layer maps each kind to its status (ADR 0005). */
public abstract sealed class BidRejectedException extends RuntimeException
		permits SellerCannotBidException, AuctionClosedException, AlreadyHighestBidderException,
		BidBelowMinimumException {

	protected BidRejectedException(String message) {
		super(message);
	}

}
