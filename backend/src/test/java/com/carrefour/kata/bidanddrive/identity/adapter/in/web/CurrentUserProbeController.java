package com.carrefour.kata.bidanddrive.identity.adapter.in.web;

import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint that echoes the user the resolver produced. */
@RestController
class CurrentUserProbeController {

	@GetMapping("/api/probe/whoami")
	String whoami(@CurrentUser UserId userId) {
		return String.valueOf(userId.value());
	}

	@GetMapping("/api/probe/whoami-optional")
	String whoamiOptional(@CurrentUser(required = false) UserId userId) {
		return userId == null ? "anonymous" : String.valueOf(userId.value());
	}

}
