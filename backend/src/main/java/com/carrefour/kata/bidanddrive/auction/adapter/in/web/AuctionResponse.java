package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.AuctionDetail;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionOutcome;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionPhase;
import com.carrefour.kata.bidanddrive.auction.domain.BidderStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 006 detail shape. The 004 create response uses it too (ADR 0010).
 * {@code myBid} (spec 008), {@code outcome} and {@code winnerName} (spec 011) are omitted, not null, when absent.
 * {@code status} is the derived phase, never the stored status (ADR 0016).
 */
public record AuctionResponse(
		Long id,
		AuctionPhase status,
		CarResponse car,
		SellerResponse seller,
		long sellerId,
		String startingPrice,
		String minIncrement,
		String currentBid,
		String minNextBid,
		int bidCount,
		Long highestBidderId,
		Instant endsAt,
		List<BidResponse> bids,
		Map<String, Link> _links,
		@JsonInclude(JsonInclude.Include.NON_NULL) MyBid myBid,
		@JsonInclude(JsonInclude.Include.NON_NULL) AuctionOutcome outcome,
		@JsonInclude(JsonInclude.Include.NON_NULL) String winnerName) {

	public static AuctionResponse of(AuctionDetail detail, URI self) {
		Auction auction = detail.auction();
		Map<String, Link> links = new LinkedHashMap<>();
		links.put("self", new Link(self.toString()));
		if (detail.phase() == AuctionPhase.OPEN) {
			links.put("place-bid", new Link(self + "/bids"));
		}
		return new AuctionResponse(
				auction.id(),
				detail.phase(),
				CarResponse.from(auction.car()),
				new SellerResponse(detail.sellerName()),
				auction.sellerId().value(),
				text(auction.startingPrice()),
				text(auction.minIncrement()),
				text(auction.currentBid()),
				text(auction.minNextBid()),
				auction.bidCount(),
				auction.highestBidderId().map(UserId::value).orElse(null),
				auction.endsAt(),
				detail.bids().stream().map(BidResponse::from).toList(),
				links,
				detail.myBid() == null ? null : new MyBid(text(detail.myBid().myHighestBid()), detail.myBidStatus()),
				detail.outcome(),
				detail.winnerName());
	}

	private static String text(Money money) {
		return money.amount().toPlainString();
	}

	public record CarResponse(
			String make,
			String model,
			int year,
			int mileageKm,
			String description,
			String imageUrl) {

		static CarResponse from(Car car) {
			return new CarResponse(car.make(), car.model(), car.year(), car.mileageKm(), car.description(),
					car.imageUrl());
		}
	}

	public record SellerResponse(String displayName) {}

	/** One entry of the bid history. The bidder is shown only by a masked name (spec 006). */
	public record BidResponse(String amount, Instant placedAt, String bidderName) {

		static BidResponse from(AuctionDetail.HistoryEntry entry) {
			return new BidResponse(text(entry.amount()), entry.placedAt(), entry.bidderName());
		}
	}

	/** The viewer's own position on this auction (spec 008). */
	public record MyBid(String highestAmount, BidderStatus status) {}

	public record Link(String href) {}

}
