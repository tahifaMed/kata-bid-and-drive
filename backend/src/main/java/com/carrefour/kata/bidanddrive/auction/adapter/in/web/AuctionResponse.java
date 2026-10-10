package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.application.AuctionDetail;
import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import com.carrefour.kata.bidanddrive.auction.domain.AuctionStatus;
import com.carrefour.kata.bidanddrive.auction.domain.Car;
import com.carrefour.kata.bidanddrive.auction.domain.Money;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The 006 detail shape. The 004 create response uses it too (ADR 0010). */
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

	public static AuctionResponse of(AuctionDetail detail, URI self) {
		Auction auction = detail.auction();
		Map<String, Link> links = new LinkedHashMap<>();
		links.put("self", new Link(self.toString()));
		if (detail.biddable()) {
			links.put("place-bid", new Link(self + "/bids"));
		}
		return new AuctionResponse(
				auction.id(),
				auction.status(),
				CarResponse.from(auction.car()),
				new SellerResponse(detail.sellerName()),
				text(auction.startingPrice()),
				text(auction.minIncrement()),
				text(auction.currentBid()),
				text(auction.minNextBid()),
				auction.bidCount(),
				auction.highestBidderId().map(UserId::value).orElse(null),
				auction.endsAt(),
				detail.bids().stream().map(BidResponse::from).toList(),
				links);
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

	public record Link(String href) {}

}
