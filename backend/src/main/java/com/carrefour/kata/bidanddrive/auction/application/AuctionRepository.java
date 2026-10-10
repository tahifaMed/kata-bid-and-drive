package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;

/** Port for storing auctions. Implemented in the persistence adapter. */
public interface AuctionRepository {

	/** Stores the auction and its car. Returns the auction with its generated id. */
	Auction save(Auction auction);

}
