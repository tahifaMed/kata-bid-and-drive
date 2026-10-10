package com.carrefour.kata.bidanddrive.auction.domain;

import java.math.BigDecimal;

/** A positive amount with two decimals. Stored as {@code NUMERIC(12,2)}, so at most 9999999999.99. */
public record Money(BigDecimal amount) {

	/** An amount sent as text: positive, at most ten integer digits, at most two decimals (trailing zeros are allowed). */
	public static final String TEXT_PATTERN = "^(?!0+(\\.0+)?$)\\d{1,10}(\\.\\d{1,2}0*)?$";
	public static final String TEXT_RULE = "must be a positive amount with at most two decimals";

	private static final BigDecimal MAX = new BigDecimal("9999999999.99");

	public static Money positive(String field, BigDecimal value) {
		if (value == null || value.signum() <= 0) {
			throw new InvalidAuctionException(field, "must be greater than zero");
		}
		if (value.stripTrailingZeros().scale() > 2) {
			throw new InvalidAuctionException(field, "must have at most two decimals");
		}
		if (value.compareTo(MAX) > 0) {
			throw new InvalidAuctionException(field, "must be at most 9999999999.99");
		}
		return new Money(value.setScale(2));
	}

}
