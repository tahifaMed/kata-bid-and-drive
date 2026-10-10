package com.carrefour.kata.bidanddrive.auction.application;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionWindow;

import java.time.Clock;

/** Opens a new auction for the seller. Wiring is added with the persistence slice. */
public class CreateAuction {

	private final AuctionRepository auctions;
	private final Clock clock;
	private final AuctionWindow window;

	public CreateAuction(AuctionRepository auctions, Clock clock, AuctionWindow window) {
		this.auctions = auctions;
		this.clock = clock;
		this.window = window;
	}

	public Auction execute(CreateAuctionCommand command) {
		Auction auction = Auction.open(
				command.sellerId(),
				command.car(),
				command.startingPrice(),
				command.minIncrement(),
				command.endsAt(),
				clock.instant(),
				window);
		return auctions.save(auction);
	}

}
