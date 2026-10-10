package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidHistory;
import com.carrefour.kata.bidanddrive.auction.domain.BidderName;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.time.Clock;
import java.util.List;

/** Reads one auction with its seller and its newest bids. Public: no identity is needed. */
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

	public AuctionDetail execute(long id) {
		Auction auction = auctions.findById(id).orElseThrow(() -> new AuctionNotFoundException(id));
		List<AuctionDetail.HistoryEntry> history = BidHistory.newest(bids.findByAuctionId(id)).stream()
				.map(this::toEntry)
				.toList();
		return new AuctionDetail(auction, displayName(auction.sellerId()), history,
				auction.isBiddableAt(clock.instant()));
	}

	private AuctionDetail.HistoryEntry toEntry(Bid bid) {
		return new AuctionDetail.HistoryEntry(bid.amount(), bid.placedAt(),
				BidderName.mask(displayName(bid.bidderId())));
	}

	/** Sellers and bidders reference {@code app_user}, so a missing user means broken data. */
	private String displayName(UserId id) {
		return users.findById(id)
				.map(User::displayName)
				.orElseThrow(() -> new IllegalStateException("user " + id.value() + " does not exist"));
	}

}
