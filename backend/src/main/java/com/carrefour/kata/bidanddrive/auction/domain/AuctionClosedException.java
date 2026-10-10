package com.carrefour.kata.bidanddrive.auction.domain;

public final class AuctionClosedException extends BidRejectedException {

	public AuctionClosedException() {
		super("The auction is closed");
	}

}
