package com.carrefour.kata.bidanddrive.identity.adapter.in.web;

import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

	static final String HEADER = "X-User-Id";

	private final UserDirectory userDirectory;

	CurrentUserArgumentResolver(UserDirectory userDirectory) {
		this.userDirectory = userDirectory;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUser.class)
				&& parameter.getParameterType() == UserId.class;
	}

	@Override
	public UserId resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		try {
			return resolve(webRequest.getHeader(HEADER));
		} catch (UnauthorizedUserException anonymous) {
			if (parameter.getParameterAnnotation(CurrentUser.class).required()) {
				throw anonymous;
			}
			return null;
		}
	}

	/** A malformed id is rejected before the user list is queried. */
	private UserId resolve(String header) {
		var userId = parse(header);
		return userDirectory.findById(userId)
				.map(user -> user.id())
				.orElseThrow(UnauthorizedUserException::new);
	}

	private static UserId parse(String header) {
		if (header == null) {
			throw new UnauthorizedUserException();
		}
		try {
			return new UserId(Long.parseLong(header.strip()));
		} catch (IllegalArgumentException invalid) {
			throw new UnauthorizedUserException();
		}
	}

}
