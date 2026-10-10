package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import java.util.List;
import java.util.Map;

/** A HAL page of open auctions (spec 005). Totals are not returned (Q3). */
public record AuctionListResponse(
		Embedded _embedded,
		PageInfo page,
		Map<String, AuctionResponse.Link> _links) {

	public record Embedded(List<AuctionListItem> auctions) {}

	public record PageInfo(int size, int number) {}

}
