package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;

import java.time.Clock;

/** Lists the auctions that are open now, one page at a time. */
public class ListOpenAuctions {

	private final AuctionRepository auctions;
	private final Clock clock;

	public ListOpenAuctions(AuctionRepository auctions, Clock clock) {
		this.auctions = auctions;
		this.clock = clock;
	}

	public AuctionPage<Auction> execute(PageQuery query) {
		return auctions.findOpen(clock.instant(), query);
	}

}
