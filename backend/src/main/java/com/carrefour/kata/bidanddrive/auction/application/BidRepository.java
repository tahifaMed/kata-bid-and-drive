package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.util.List;
import java.util.Optional;

/** Port for reading bids. Bids are written with their auction, see {@link AuctionRepository#recordBid}. */
public interface BidRepository {

	/** At most {@code limit} of the auction's newest bids, by {@code placedAt} then id, both descending. */
	List<Bid> findNewest(long auctionId, int limit);

	/** The bid, if it belongs to that auction. */
	Optional<Bid> findById(long auctionId, long bidId);

	/** The bidder's standing on one auction, read like {@link #findStandings}. Empty if they never bid there. */
	Optional<BidderStanding> findStanding(long auctionId, UserId bidder);

	/**
	 * Every auction the bidder has bid on, open or ended, with their highest bid there, sorted by end time,
	 * then id. The auction and the bids are read together, so the status matches the amounts.
	 */
	List<BidderStanding> findStandings(UserId bidder);

}
