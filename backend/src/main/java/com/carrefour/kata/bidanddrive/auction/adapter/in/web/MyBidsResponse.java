package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import java.util.List;
import java.util.Map;

/** A HAL collection of the caller's bids (spec 008). Not paged: one buyer's bids are few. */
public record MyBidsResponse(Embedded _embedded, Map<String, AuctionResponse.Link> _links) {

	public record Embedded(List<MyBidItem> myBids) {}

}
