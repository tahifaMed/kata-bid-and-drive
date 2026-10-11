package com.carrefour.kata.bidanddrive.auction.domain;

import java.time.Instant;

/** Something that happened to an auction, sent to its live subscribers after commit (spec 009). */
public sealed interface AuctionEvent permits BidPlaced {

	long auctionId();

	Instant occurredAt();

}
