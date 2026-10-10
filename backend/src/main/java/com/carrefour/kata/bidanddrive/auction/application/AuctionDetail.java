package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Money;

import java.time.Instant;
import java.util.List;

/** What the auction detail shows (spec 006). {@code biddable} decides the {@code place-bid} link. */
public record AuctionDetail(Auction auction, String sellerName, List<HistoryEntry> bids, boolean biddable) {

	public AuctionDetail {
		bids = List.copyOf(bids);
	}

	/** One bid in the public history. The bidder is shown only by a masked name. */
	public record HistoryEntry(Money amount, Instant placedAt, String bidderName) {}

}
