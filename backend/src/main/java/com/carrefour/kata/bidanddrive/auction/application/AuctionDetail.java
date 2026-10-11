package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.Money;

import java.time.Instant;
import java.util.List;

/**
 * What the auction detail shows (spec 006). {@code biddable} decides the {@code place-bid} link.
 * {@code myBid} is the viewer's standing (spec 008), null for an anonymous viewer or one who never bid here.
 */
public record AuctionDetail(Auction auction, String sellerName, List<HistoryEntry> bids, boolean biddable,
		BidderStanding myBid) {

	public AuctionDetail {
		bids = List.copyOf(bids);
	}

	/** The detail as anyone sees it, with no viewer standing. */
	public AuctionDetail(Auction auction, String sellerName, List<HistoryEntry> bids, boolean biddable) {
		this(auction, sellerName, bids, biddable, null);
	}

	/** One bid in the public history. The bidder is shown only by a masked name. */
	public record HistoryEntry(Money amount, Instant placedAt, String bidderName) {}

}
