package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionOutcome;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionPhase;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Money;

import java.time.Instant;
import java.util.List;

/**
 * What the auction detail shows (spec 006), computed at one instant. {@code phase} decides the {@code place-bid}
 * link. {@code outcome} is null while open, and {@code winnerName} (masked) is null unless sold (spec 011).
 * {@code myBid} is the viewer's standing (spec 008), null for an anonymous viewer or one who never bid here,
 * and {@code myBidStatus} is its status at that same instant.
 */
public record AuctionDetail(Auction auction, String sellerName, List<HistoryEntry> bids, AuctionPhase phase,
		AuctionOutcome outcome, String winnerName, BidderStanding myBid, BidderStatus myBidStatus) {

	public AuctionDetail {
		bids = List.copyOf(bids);
	}

	/** An open auction as anyone sees it, with no viewer standing. */
	public static AuctionDetail open(Auction auction, String sellerName, List<HistoryEntry> bids) {
		return new AuctionDetail(auction, sellerName, bids, AuctionPhase.OPEN, null, null, null, null);
	}

	/** One bid in the public history. The bidder is shown only by a masked name. */
	public record HistoryEntry(Money amount, Instant placedAt, String bidderName) {}

}
