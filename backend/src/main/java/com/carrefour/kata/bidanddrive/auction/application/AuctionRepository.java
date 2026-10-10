package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;

import java.time.Instant;
import java.util.Optional;

/** Port for storing and reading auctions. Implemented in the persistence adapter. */
public interface AuctionRepository {

	/** Stores the auction and its car. Returns the auction with its generated id. */
	Auction save(Auction auction);

	/** Auctions with status OPEN and an end time after {@code now}, sorted by end time, then id. */
	AuctionPage<Auction> findOpen(Instant now, PageQuery query);

	/** The auction with its car, whatever its status or end time. */
	Optional<Auction> findById(long id);

}
