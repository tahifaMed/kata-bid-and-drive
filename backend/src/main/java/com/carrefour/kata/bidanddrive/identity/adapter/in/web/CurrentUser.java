package com.carrefour.kata.bidanddrive.identity.adapter.in.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller parameter of type UserId that is filled from the X-User-Id header.
 * When required, a missing or unknown id is a 401. When not required, it is null instead (ADR 0014).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {

	boolean required() default true;

}
