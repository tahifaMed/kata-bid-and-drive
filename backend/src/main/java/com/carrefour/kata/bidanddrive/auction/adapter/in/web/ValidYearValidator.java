package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import com.carrefour.kata.bidanddrive.auction.domain.Auction;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;

@Component
class ValidYearValidator implements ConstraintValidator<ValidYear, Integer> {

	private final Clock clock;

	ValidYearValidator(Clock clock) {
		this.clock = clock;
	}

	@Override
	public boolean isValid(Integer year, ConstraintValidatorContext context) {
		if (year == null) {
			return true;
		}
		int latest = Year.now(clock).getValue() + 1;
		return year >= Auction.FIRST_YEAR && year <= latest;
	}

}
