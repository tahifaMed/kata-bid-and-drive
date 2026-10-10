package com.carrefour.kata.bidanddrive.auction.domain;

import java.util.Comparator;
import java.util.List;

/** The bid history shown on the auction detail (spec 006). */
public final class BidHistory {

	public static final int LIMIT = 50;

	private static final Comparator<Bid> NEWEST_FIRST = Comparator.comparing(Bid::placedAt)
			.thenComparingLong(Bid::id)
			.reversed();

	private BidHistory() {
	}

	/** The {@value #LIMIT} newest bids, newest first. Bids placed at the same instant are ordered by id. */
	public static List<Bid> newest(List<Bid> bids) {
		return bids.stream().sorted(NEWEST_FIRST).limit(LIMIT).toList();
	}

}
