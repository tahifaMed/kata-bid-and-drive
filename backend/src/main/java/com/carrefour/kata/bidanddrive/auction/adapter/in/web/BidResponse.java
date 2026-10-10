package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.domain.Bid;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** A placed bid. The {@code bid-placed} event (009) calls {@code id} {@code bidId}. */
record BidResponse(
		long id,
		long auctionId,
		long bidderId,
		String amount,
		Instant placedAt,
		Map<String, AuctionResponse.Link> _links) {

	static BidResponse of(Bid bid, URI self, URI auction) {
		Map<String, AuctionResponse.Link> links = new LinkedHashMap<>();
		links.put("self", new AuctionResponse.Link(self.toString()));
		links.put("auction", new AuctionResponse.Link(auction.toString()));
		return new BidResponse(bid.id(), bid.auctionId(), bid.bidderId().value(),
				bid.amount().amount().toPlainString(), bid.placedAt(), links);
	}

}
