package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.Car;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/** One open auction in the list (spec 005). No seller, reserve or description: the list is a summary. */
public record AuctionListItem(
		Long id,
		CarSummary car,
		String currentBid,
		String minNextBid,
		int bidCount,
		Instant endsAt,
		Map<String, AuctionResponse.Link> _links) {

	public static AuctionListItem of(Auction auction, URI self) {
		return new AuctionListItem(
				auction.id(),
				CarSummary.from(auction.car()),
				auction.currentBid().amount().toPlainString(),
				auction.minNextBid().amount().toPlainString(),
				auction.bidCount(),
				auction.endsAt(),
				Map.of("self", new AuctionResponse.Link(self.toString())));
	}

	public record CarSummary(String make, String model, int year, String imageUrl) {

		static CarSummary from(Car car) {
			return new CarSummary(car.make(), car.model(), car.year(), car.imageUrl());
		}

	}

}
