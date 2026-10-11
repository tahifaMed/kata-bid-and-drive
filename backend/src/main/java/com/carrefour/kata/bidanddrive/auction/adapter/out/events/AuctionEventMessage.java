package com.carrefour.kata.bidanddrive.auction.adapter.out.events;

import com.carrefour.kata.bidanddrive.auction.domain.AuctionEvent;
import com.carrefour.kata.bidanddrive.auction.domain.BidPlaced;
import com.carrefour.kata.bidanddrive.auction.domain.Money;

import java.time.Instant;

/**
 * The envelope sent as an event's {@code data:} line (spec 009). {@code type} is also the SSE event name.
 * Money is a plain string (ADR 0004).
 */
record AuctionEventMessage(String type, long auctionId, Instant occurredAt, Object data) {

	static AuctionEventMessage of(AuctionEvent event) {
		return switch (event) {
			case BidPlaced bid -> new AuctionEventMessage("bid-placed", bid.auctionId(), bid.occurredAt(),
					BidPlacedData.of(bid));
		};
	}

	record BidPlacedData(
			long bidId,
			String amount,
			long bidderId,
			String bidderName,
			long highestBidderId,
			String currentBid,
			String minNextBid,
			int bidCount,
			Instant endsAt,
			boolean extended) {

		static BidPlacedData of(BidPlaced bid) {
			return new BidPlacedData(bid.bidId(), text(bid.amount()), bid.bidderId().value(), bid.bidderName(),
					bid.highestBidderId().value(), text(bid.currentBid()), text(bid.minNextBid()), bid.bidCount(),
					bid.endsAt(), bid.extended());
		}

		private static String text(Money money) {
			return money.amount().toPlainString();
		}

	}

}
