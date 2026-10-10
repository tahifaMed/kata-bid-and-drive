package com.carrefour.kata.bidanddrive.identity.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CurrentUserProbeController.class)
class CurrentUserResolverTests {

	private static final String PROBE = "/api/probe/whoami";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserDirectory userDirectory;

	@Test
	void runs_as_the_user_named_in_the_header() throws Exception {
		given(userDirectory.findById(new UserId(2))).willReturn(Optional.of(new User(new UserId(2), "Bob")));

		mockMvc.perform(get(PROBE).header("X-User-Id", "2"))
				.andExpect(status().isOk())
				.andExpect(content().string("2"));
	}

	@Test
	void rejects_a_missing_header_with_a_401_problem_detail() throws Exception {
		mockMvc.perform(get(PROBE))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.title").value("Unauthorized"));
	}

	@Test
	void rejects_a_header_that_matches_no_user() throws Exception {
		given(userDirectory.findById(new UserId(99))).willReturn(Optional.empty());

		mockMvc.perform(get(PROBE).header("X-User-Id", "99"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void rejects_a_header_that_is_not_a_positive_number_without_looking_up_users() throws Exception {
		mockMvc.perform(get(PROBE).header("X-User-Id", "abc"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(PROBE).header("X-User-Id", "0"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(userDirectory);
	}

}
