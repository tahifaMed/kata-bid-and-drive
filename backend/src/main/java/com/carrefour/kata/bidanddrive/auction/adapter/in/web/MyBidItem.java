package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStatus;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/** One auction the caller bid on (spec 008). It is not a resource of its own, so it links to its auction. */
public record MyBidItem(
		long auctionId,
		AuctionListItem.CarSummary car,
		String myHighestBid,
		String currentBid,
		BidderStatus status,
		Instant endsAt,
		Map<String, AuctionResponse.Link> _links) {

	public static MyBidItem of(BidderStanding standing, URI auction) {
		Auction bidOn = standing.auction();
		return new MyBidItem(
				bidOn.id(),
				AuctionListItem.CarSummary.from(bidOn.car()),
				standing.myHighestBid().amount().toPlainString(),
				bidOn.currentBid().amount().toPlainString(),
				standing.status(),
				bidOn.endsAt(),
				Map.of("auction", new AuctionResponse.Link(auction.toString())));
	}

}
