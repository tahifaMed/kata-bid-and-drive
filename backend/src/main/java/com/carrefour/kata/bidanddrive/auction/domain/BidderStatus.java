package com.carrefour.kata.bidanddrive.auction.domain;

/** Where a bidder stands on an auction they bid on. WON and LOST arrive with auto-close (spec 011). */
public enum BidderStatus {
	WINNING,
	OUTBID
}
