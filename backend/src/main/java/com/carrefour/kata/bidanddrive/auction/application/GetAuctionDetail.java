package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionOutcome;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidHistory;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStanding;
import com.carrefour.kata.bidanddrive.auction.domain.BidderName;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads one auction with its seller and its newest bids. Public: no identity is needed.
 * A known viewer also gets their own standing on it (spec 008).
 */
public class GetAuctionDetail {

	private final AuctionRepository auctions;
	private final BidRepository bids;
	private final UserDirectory users;
	private final Clock clock;

	public GetAuctionDetail(AuctionRepository auctions, BidRepository bids, UserDirectory users, Clock clock) {
		this.auctions = auctions;
		this.bids = bids;
		this.users = users;
		this.clock = clock;
	}

	/**
	 * The detail plus the viewer's standing. When they bid here, the auction comes from the same read as
	 * their standing, so {@code currentBid}, {@code highestBidderId} and {@code myBid} agree.
	 */
	public AuctionDetail execute(long id, UserId viewer) {
		return bids.findStanding(id, viewer)
				.map(standing -> detailOf(standing.auction(), standing))
				.orElseGet(() -> execute(id));
	}

	public AuctionDetail execute(long id) {
		return detailOf(auctions.findById(id).orElseThrow(() -> new AuctionNotFoundException(id)), null);
	}

	/** Everything time-dependent is computed at one instant, so status, outcome and myBid agree (spec 011). */
	private AuctionDetail detailOf(Auction auction, BidderStanding myBid) {
		long id = auction.id();
		Instant now = clock.instant();
		// The port already returns the newest bids; BidHistory keeps the order and cap whatever the adapter does.
		List<Bid> newest = BidHistory.newest(bids.findNewest(id, BidHistory.LIMIT));
		Map<UserId, String> maskedNames = new HashMap<>();
		List<AuctionDetail.HistoryEntry> history = newest.stream()
				.map(bid -> new AuctionDetail.HistoryEntry(bid.amount(), bid.placedAt(),
						maskedNames.computeIfAbsent(bid.bidderId(), bidder -> BidderName.mask(displayName(bidder)))))
				.toList();
		AuctionOutcome outcome = auction.outcomeAt(now).orElse(null);
		String winnerName = outcome == AuctionOutcome.SOLD
				? auction.highestBidderId()
						.map(winner -> maskedNames.computeIfAbsent(winner, w -> BidderName.mask(displayName(w))))
						.orElseThrow()
				: null;
		return new AuctionDetail(auction, displayName(auction.sellerId()), history, auction.phaseAt(now),
				outcome, winnerName, myBid, myBid == null ? null : myBid.status(now));
	}

	/** Sellers and bidders reference {@code app_user}, so a missing user means broken data. */
	private String displayName(UserId id) {
		return users.findById(id)
				.map(User::displayName)
				.orElseThrow(() -> new IllegalStateException("user " + id.value() + " does not exist"));
	}

}
