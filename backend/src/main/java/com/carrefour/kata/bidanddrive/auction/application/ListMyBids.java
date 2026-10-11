package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Lists every auction the caller has bid on, open or ended, with where they stand (spec 008).
 * Every status is computed at one instant, so WON and LOST appear together once an auction ends (spec 011).
 */
public class ListMyBids {

	private final BidRepository bids;
	private final Clock clock;

	public ListMyBids(BidRepository bids, Clock clock) {
		this.bids = bids;
		this.clock = clock;
	}

	public List<MyBidView> execute(UserId bidder) {
		Instant now = clock.instant();
		return bids.findStandings(bidder).stream()
				.map(standing -> new MyBidView(standing, standing.status(now)))
				.toList();
	}

}
