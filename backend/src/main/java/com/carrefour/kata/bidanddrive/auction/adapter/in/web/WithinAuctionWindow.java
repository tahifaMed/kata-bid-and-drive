package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** An end time inside the configured auction window (see ADR 0008). */
@Documented
@Constraint(validatedBy = WithinAuctionWindowValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface WithinAuctionWindow {

	String message() default "is outside the allowed window";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
