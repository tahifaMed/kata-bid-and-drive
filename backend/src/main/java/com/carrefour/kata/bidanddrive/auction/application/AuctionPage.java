package com.carrefour.kata.bidanddrive.auction.application;

import java.util.List;

/** One page of items. {@code hasNext} says whether a later page exists, so no count query is needed. */
public record AuctionPage<T>(List<T> items, int number, int size, boolean hasNext) {

	public AuctionPage {
		items = List.copyOf(items);
	}

}
