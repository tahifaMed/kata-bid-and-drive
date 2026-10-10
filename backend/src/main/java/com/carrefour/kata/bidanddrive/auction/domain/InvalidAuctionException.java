package com.carrefour.kata.bidanddrive.auction.domain;

/** A rule about an auction or its car was broken. {@code field} is the JSON path of the offending input. */
public class InvalidAuctionException extends RuntimeException {

	private final String field;

	public InvalidAuctionException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}

}
