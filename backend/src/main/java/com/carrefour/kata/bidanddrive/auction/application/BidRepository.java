package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Bid;

import java.util.List;

/** Port for reading bids. Implemented in the persistence adapter. */
public interface BidRepository {

	/** At most {@code limit} of the auction's newest bids, by {@code placedAt} then id, both descending. */
	List<Bid> findNewest(long auctionId, int limit);

}
