package com.carrefour.kata.bidanddrive.auction.domain;

/** Where a bidder stands on an auction they bid on: WINNING or OUTBID while open, WON or LOST once closed. */
public enum BidderStatus {
	WINNING,
	OUTBID,
	WON,
	LOST
}
