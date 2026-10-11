package com.carrefour.kata.bidanddrive.auction.domain;

/** How a closed auction ended. There is no reserve, so any bid sells the car (spec 011). */
public enum AuctionOutcome {
	SOLD,
	NO_BIDS
}
