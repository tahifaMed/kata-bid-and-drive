package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.domain.AuctionWindow;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
class WithinAuctionWindowValidator implements ConstraintValidator<WithinAuctionWindow, Instant> {

	private final AuctionWindow window;
	private final Clock clock;

	WithinAuctionWindowValidator(AuctionWindow window, Clock clock) {
		this.window = window;
		this.clock = clock;
	}

	@Override
	public boolean isValid(Instant endsAt, ConstraintValidatorContext context) {
		return endsAt == null || window.contains(endsAt, clock.instant());
	}

}
