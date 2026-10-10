package com.carrefour.kata.bidanddrive.auction.domain;

public final class AlreadyHighestBidderException extends BidRejectedException {

	public AlreadyHighestBidderException() {
		super("You are already the highest bidder");
	}

}
