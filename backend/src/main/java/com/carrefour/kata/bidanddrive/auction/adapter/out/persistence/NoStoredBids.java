package com.carrefour.kata.bidanddrive.auction.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.auction.application.BidRepository;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import org.springframework.stereotype.Component;

import java.util.List;

/** There is no bid table until spec 007, which replaces this adapter with a JPA one. */
@Component
class NoStoredBids implements BidRepository {

	@Override
	public List<Bid> findNewest(long auctionId, int limit) {
		return List.of();
	}

}
