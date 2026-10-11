package com.carrefour.kata.bidanddrive.auction.domain;

/** Whether an auction still takes bids. Derived from {@code endsAt} on read, never stored (ADR 0016). */
public enum AuctionPhase {
	OPEN,
	CLOSED
}
