package com.carrefour.kata.bidanddrive.identity.domain;

public record UserId(long value) {

	public UserId {
		if (value <= 0) {
			throw new IllegalArgumentException("user id must be positive: " + value);
		}
	}

}
