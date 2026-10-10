package com.carrefour.kata.bidanddrive.auction.domain;

import java.net.URI;

/** The car being sold. The year range depends on the clock, so {@link Auction} checks it. */
public record Car(String make, String model, int year, int mileageKm, String description, String imageUrl) {

	public Car {
		requireText("car.make", make, 60);
		requireText("car.model", model, 60);
		if (mileageKm < 0) {
			throw new InvalidAuctionException("car.mileageKm", "must be zero or more");
		}
		if (description != null && description.length() > 2000) {
			throw new InvalidAuctionException("car.description", "must be at most 2000 characters");
		}
		if (imageUrl != null) {
			requireHttpUrl(imageUrl);
		}
	}

	private static void requireText(String field, String value, int max) {
		if (value == null || value.isBlank()) {
			throw new InvalidAuctionException(field, "is required");
		}
		if (value.length() > max) {
			throw new InvalidAuctionException(field, "must be at most " + max + " characters");
		}
	}

	private static void requireHttpUrl(String value) {
		if (value.length() > 2048) {
			throw new InvalidAuctionException("car.imageUrl", "must be at most 2048 characters");
		}
		URI uri;
		try {
			uri = URI.create(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidAuctionException("car.imageUrl", "must be an http or https URL");
		}
		String scheme = uri.getScheme();
		if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
			throw new InvalidAuctionException("car.imageUrl", "must be an http or https URL");
		}
	}

}
