package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Bid;

import java.util.List;
import java.util.Optional;

/** Port for reading bids. Bids are written with their auction, see {@link AuctionRepository#recordBid}. */
public interface BidRepository {

	/** At most {@code limit} of the auction's newest bids, by {@code placedAt} then id, both descending. */
	List<Bid> findNewest(long auctionId, int limit);

	/** The bid, if it belongs to that auction. */
	Optional<Bid> findById(long auctionId, long bidId);

}
