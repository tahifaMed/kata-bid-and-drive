package com.carrefour.kata.bidanddrive.identity.adapter.in.web;

import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class IdentityWebConfiguration implements WebMvcConfigurer {

	private final UserDirectory userDirectory;

	IdentityWebConfiguration(UserDirectory userDirectory) {
		this.userDirectory = userDirectory;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CurrentUserArgumentResolver(userDirectory));
	}

}
