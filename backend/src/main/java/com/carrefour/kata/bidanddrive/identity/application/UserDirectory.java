package com.carrefour.kata.bidanddrive.identity.application;

import com.carrefour.kata.bidanddrive.identity.domain.User;
import java.util.List;

public interface UserDirectory {

	List<User> findAll();

}
