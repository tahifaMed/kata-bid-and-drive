package com.carrefour.kata.bidanddrive.auction.domain;

public final class SellerCannotBidException extends BidRejectedException {

	public SellerCannotBidException() {
		super("The seller cannot bid on their own auction");
	}

}
