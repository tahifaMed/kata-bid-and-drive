package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.util.List;

/** Lists every auction the caller has bid on, open or ended, with where they stand (spec 008). */
public class ListMyBids {

	private final BidRepository bids;

	public ListMyBids(BidRepository bids) {
		this.bids = bids;
	}

	public List<BidderStanding> execute(UserId bidder) {
		return bids.findStandings(bidder);
	}

}
