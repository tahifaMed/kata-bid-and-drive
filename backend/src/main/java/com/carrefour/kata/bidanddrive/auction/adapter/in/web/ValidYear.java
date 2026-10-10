package com.carrefour.kata.bidanddrive.auction.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A car year from 1886 to next year, measured with the injected clock. */
@Documented
@Constraint(validatedBy = ValidYearValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidYear {

	String message() default "must be from 1886 to next year";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
