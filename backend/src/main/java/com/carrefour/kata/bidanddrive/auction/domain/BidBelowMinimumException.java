package com.carrefour.kata.bidanddrive.auction.domain;

public final class BidBelowMinimumException extends BidRejectedException {

	private final Money minimumBid;

	public BidBelowMinimumException(Money minimumBid) {
		super("The bid must be at least " + minimumBid.amount().toPlainString());
		this.minimumBid = minimumBid;
	}

	public Money minimumBid() {
		return minimumBid;
	}

}
