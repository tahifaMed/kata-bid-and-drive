package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;

/** Reads one bid for its own bidder. Anyone else gets "not found", so ids cannot be walked (ADR 0013). */
public class GetBid {

	private final BidRepository bids;

	public GetBid(BidRepository bids) {
		this.bids = bids;
	}

	public Bid execute(long auctionId, long bidId, UserId viewer) {
		return bids.findById(auctionId, bidId)
				.filter(bid -> bid.bidderId().equals(viewer))
				.orElseThrow(() -> new BidNotFoundException(auctionId, bidId));
	}

}
