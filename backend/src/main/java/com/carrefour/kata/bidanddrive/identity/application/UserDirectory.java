package com.carrefour.kata.bidanddrive.identity.application;

import com.carrefour.kata.bidanddrive.identity.domain.User;
import com.carrefour.kata.bidanddrive.identity.domain.UserId;
import java.util.List;
import java.util.Optional;

public interface UserDirectory {

	List<User> findAll();

	Optional<User> findById(UserId id);

}
