package com.carrefour.kata.bidanddrive.identity.adapter.in.web;

import com.carrefour.kata.bidanddrive.identity.application.ListUsers;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserController {

	private final ListUsers listUsers;

	UserController(ListUsers listUsers) {
		this.listUsers = listUsers;
	}

	@GetMapping
	List<UserResponse> list() {
		return listUsers.execute().stream().map(UserResponse::from).toList();
	}

}
