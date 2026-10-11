package com.carrefour.kata.bidanddrive.auction.application;

/** Turns away an unknown auction before its event stream opens (spec 009). Any status may be watched. */
public class CheckAuctionExists {

	private final AuctionRepository auctions;

	public CheckAuctionExists(AuctionRepository auctions) {
		this.auctions = auctions;
	}

	public void execute(long auctionId) {
		if (!auctions.existsById(auctionId)) {
			throw new AuctionNotFoundException(auctionId);
		}
	}

}
