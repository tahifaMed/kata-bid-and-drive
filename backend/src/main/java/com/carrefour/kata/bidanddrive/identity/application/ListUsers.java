package com.carrefour.kata.bidanddrive.identity.application;

import com.carrefour.kata.bidanddrive.identity.domain.User;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListUsers {

	private final UserDirectory userDirectory;

	public ListUsers(UserDirectory userDirectory) {
		this.userDirectory = userDirectory;
	}

	public List<User> execute() {
		return userDirectory.findAll();
	}

}
