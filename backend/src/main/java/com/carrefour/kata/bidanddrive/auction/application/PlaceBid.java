package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidPlaced;
import com.carrefour.kata.bidanddrive.auction.domain.BidPlacement;
import com.carrefour.kata.bidanddrive.auction.domain.InvalidAuctionException;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Places a bid (spec 007). The amount is checked after the auction lookup, so an unknown auction
 * is a 404 even when the body is also invalid (ADR 0005). An accepted bid publishes {@code BidPlaced},
 * which is sent only once this transaction commits (spec 009).
 */
public class PlaceBid {

	private static final String FIELD = "amount";

	private final AuctionRepository auctions;
	private final Clock clock;
	private final UserDirectory users;
	private final AuctionEventPublisher events;

	public PlaceBid(AuctionRepository auctions, Clock clock, UserDirectory users, AuctionEventPublisher events) {
		this.auctions = auctions;
		this.clock = clock;
		this.users = users;
		this.events = events;
	}

	@Transactional
	public Bid execute(long auctionId, UserId bidder, RawAmount amount) {
		Auction auction = auctions.findById(auctionId).orElseThrow(() -> new AuctionNotFoundException(auctionId));
		BidPlacement placement = auction.placeBid(bidder, parse(amount), clock.instant());
		Bid saved = auctions.recordBid(placement.auction(), placement.bid());
		events.publish(BidPlaced.of(placement.auction(), saved, displayName(bidder)));
		return saved;
	}

	/** The web layer has already turned away unknown users (ADR 0001), so a miss here is broken data. */
	private String displayName(UserId bidder) {
		return users.findById(bidder)
				.map(User::displayName)
				.orElseThrow(() -> new IllegalStateException("Bidder " + bidder.value() + " is not stored"));
	}

	private static Money parse(RawAmount amount) {
		if (amount.text() == null) {
			throw new InvalidBidException(FIELD, "is required as a string, such as \"1100.00\"");
		}
		if (!amount.text().matches(Money.TEXT_PATTERN)) {
			throw new InvalidBidException(FIELD, Money.TEXT_RULE);
		}
		try {
			return Money.positive(FIELD, new BigDecimal(amount.text()));
		} catch (InvalidAuctionException invalid) {
			throw new InvalidBidException(FIELD, invalid.getMessage());
		}
	}

}
