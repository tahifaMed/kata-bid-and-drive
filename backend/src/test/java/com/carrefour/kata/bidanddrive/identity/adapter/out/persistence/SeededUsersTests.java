package com.carrefour.kata.bidanddrive.identity.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.carrefour.kata.bidanddrive.TestcontainersConfiguration;
import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SeededUsersTests {

	private final UserDirectory userDirectory;

	@Autowired
	SeededUsersTests(UserDirectory userDirectory) {
		this.userDirectory = userDirectory;
	}

	@Test
	void migrated_database_holds_four_seeded_users_with_ids_one_to_four() {
		assertThat(userDirectory.findAll()).containsExactly(
				new User(new UserId(1), "Alice"),
				new User(new UserId(2), "Bob"),
				new User(new UserId(3), "Chloe"),
				new User(new UserId(4), "Dan"));
	}

}
