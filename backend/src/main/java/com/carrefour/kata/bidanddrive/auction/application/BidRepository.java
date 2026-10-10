package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Bid;

import java.util.List;

/** Port for reading bids. Implemented in the persistence adapter. */
public interface BidRepository {

	/** All bids of the auction, in any order. */
	List<Bid> findByAuctionId(long auctionId);

}
