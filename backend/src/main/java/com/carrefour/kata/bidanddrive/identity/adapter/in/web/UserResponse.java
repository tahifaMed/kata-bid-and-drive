package com.carrefour.kata.bidanddrive.identity.adapter.in.web;

import com.carrefour.kata.bidanddrive.identity.domain.User;

public record UserResponse(long id, String displayName) {

	static UserResponse from(User user) {
		return new UserResponse(user.id().value(), user.displayName());
	}

}
