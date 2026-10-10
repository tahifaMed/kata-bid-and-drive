package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** The 006 detail shape, as a new auction shows it (ADR 0010). */
public record AuctionResponse(
		Long id,
		AuctionStatus status,
		CarResponse car,
		SellerResponse seller,
		String startingPrice,
		String minIncrement,
		String currentBid,
		String minNextBid,
		int bidCount,
		Long highestBidderId,
		Instant endsAt,
		List<BidResponse> bids,
		Map<String, Link> _links) {

	public static AuctionResponse of(Auction auction, String sellerName, URI self) {
		String startingPrice = auction.startingPrice().amount().toPlainString();
		return new AuctionResponse(
				auction.id(),
				auction.status(),
				CarResponse.from(auction.car()),
				new SellerResponse(sellerName),
				startingPrice,
				auction.minIncrement().amount().toPlainString(),
				startingPrice,
				startingPrice,
				0,
				null,
				auction.endsAt(),
				List.of(),
				Map.of(
						"self", new Link(self.toString()),
						"place-bid", new Link(self + "/bids")));
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

	/** Bid history entries. A new auction has none; spec 006 fills this in. */
	public record BidResponse(String amount, Instant placedAt, String bidderName) {}

	public record Link(String href) {}

}
