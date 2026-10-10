package com.carrefour.kata.bidanddrive.auction.application;

/** The bid request is malformed. {@code field} is the JSON path of the offending input. */
public class InvalidBidException extends RuntimeException {

	private final String field;

	public InvalidBidException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}

}
