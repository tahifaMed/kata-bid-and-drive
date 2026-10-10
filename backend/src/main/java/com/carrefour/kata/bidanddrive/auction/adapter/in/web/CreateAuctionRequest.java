package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Money is a JSON string with at most two decimals, greater than zero (ADR 0004).
 * endsAt is an ISO-8601 time with an offset, read as a string so that a JSON number is rejected.
 */
public record CreateAuctionRequest(
		@Valid @NotNull CarRequest car,
		@NotNull @Pattern(regexp = PriceValidation.PATTERN, message = PriceValidation.MESSAGE) String startingPrice,
		@NotNull @Pattern(regexp = PriceValidation.PATTERN, message = PriceValidation.MESSAGE) String minIncrement,
		@NotNull @WithinAuctionWindow String endsAt) {

	public record CarRequest(
			@NotBlank @Size(max = 60) String make,
			@NotBlank @Size(max = 60) String model,
			@ValidYear int year,
			@NotNull @PositiveOrZero Integer mileageKm,
			@Size(max = 2000) String description,
			@Size(max = 2048) @Pattern(regexp = "(?i)^https?://[^\\s/]+\\S*$", message = "must be an http or https URL")
			String imageUrl) {}

	/** Positive, at most ten integer digits, at most two decimals (trailing zeros are allowed). */
	static final class PriceValidation {

		static final String PATTERN = "^(?!0+(\\.0+)?$)\\d{1,10}(\\.\\d{1,2}0*)?$";
		static final String MESSAGE = "must be a positive amount with at most two decimals";

		private PriceValidation() {
		}

	}

}
