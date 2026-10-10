package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;

import java.time.Instant;
import java.util.Optional;

/** Port for storing and reading auctions. Implemented in the persistence adapter. */
public interface AuctionRepository {

	/** Stores the auction and its car. Returns the auction with its generated id. */
	Auction save(Auction auction);

	/**
	 * Stores a placed bid and the auction's new totals in one transaction, and returns the bid with its id.
	 * Fails with a version conflict when the stored auction is no longer at {@code auction.version()} (ADR 0003).
	 */
	Bid recordBid(Auction auction, Bid bid);

	/** Auctions with status OPEN and an end time after {@code now}, sorted by end time, then id. */
	AuctionPage<Auction> findOpen(Instant now, PageQuery query);

	/** The auction with its car, whatever its status or end time. */
	Optional<Auction> findById(long id);

}
