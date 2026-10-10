package com.carrefour.kata.bidanddrive.identity.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserIdTests {

	@Test
	void rejects_zero_id() {
		assertThatThrownBy(() -> new UserId(0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejects_negative_id() {
		assertThatThrownBy(() -> new UserId(-1))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
