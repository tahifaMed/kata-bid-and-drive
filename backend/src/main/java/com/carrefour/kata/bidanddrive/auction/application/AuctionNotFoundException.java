package com.carrefour.kata.bidanddrive.auction.application;

public class AuctionNotFoundException extends RuntimeException {

	public AuctionNotFoundException(long id) {
		super("Auction " + id + " does not exist");
	}

}
