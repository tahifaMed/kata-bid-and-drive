package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.domain.AuctionWindow;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** Accepts only an ISO-8601 time with an offset (no offset is invalid), inside the window. */
@Component
class WithinAuctionWindowValidator implements ConstraintValidator<WithinAuctionWindow, String> {

	private final AuctionWindow window;
	private final Clock clock;

	WithinAuctionWindowValidator(AuctionWindow window, Clock clock) {
		this.window = window;
		this.clock = clock;
	}

	@Override
	public boolean isValid(String endsAt, ConstraintValidatorContext context) {
		if (endsAt == null) {
			return true;
		}
		try {
			return window.contains(OffsetDateTime.parse(endsAt).toInstant(), clock.instant());
		} catch (DateTimeParseException invalid) {
			return false;
		}
	}

}
