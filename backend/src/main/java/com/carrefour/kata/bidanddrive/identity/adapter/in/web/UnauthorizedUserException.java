package com.carrefour.kata.bidanddrive.identity.adapter.in.web;

class UnauthorizedUserException extends RuntimeException {

	UnauthorizedUserException() {
		super("X-User-Id header is missing or does not match a known user");
	}

}
