package com.carrefour.kata.bidanddrive.identity.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carrefour.kata.bidanddrive.identity.application.ListUsers;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListUsers listUsers;

	@Test
	void lists_users_as_id_and_display_name_without_a_user_header() throws Exception {
		given(listUsers.execute()).willReturn(List.of(new User(new UserId(1), "Alice")));

		mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].displayName").value("Alice"))
				.andExpect(jsonPath("$[0].userId").doesNotExist());
	}

}
