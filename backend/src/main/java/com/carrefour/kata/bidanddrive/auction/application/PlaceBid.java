package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Bid;
import com.carrefour.kata.bidanddrive.auction.domain.BidPlacement;
import com.carrefour.kata.bidanddrive.auction.domain.InvalidAuctionException;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Places a bid (spec 007). The amount is checked after the auction lookup, so an unknown auction
 * is a 404 even when the body is also invalid (ADR 0005).
 */
public class PlaceBid {

	private static final String FIELD = "amount";

	private final AuctionRepository auctions;
	private final Clock clock;

	public PlaceBid(AuctionRepository auctions, Clock clock) {
		this.auctions = auctions;
		this.clock = clock;
	}

	@Transactional
	public Bid execute(long auctionId, UserId bidder, RawAmount amount) {
		Auction auction = auctions.findById(auctionId).orElseThrow(() -> new AuctionNotFoundException(auctionId));
		BidPlacement placement = auction.placeBid(bidder, parse(amount), clock.instant());
		return auctions.recordBid(placement.auction(), placement.bid());
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
