package com.carrefour.kata.bidanddrive.identity.adapter.out.persistence;

import com.carrefour.kata.bidanddrive.identity.application.UserDirectory;
import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class JpaUserDirectory implements UserDirectory {

	private final AppUserJpaRepository repository;

	JpaUserDirectory(AppUserJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<User> findAll() {
		return repository.findAllByOrderByIdAsc().stream()
				.map(entity -> new User(new UserId(entity.getId()), entity.getDisplayName()))
				.toList();
	}

}
