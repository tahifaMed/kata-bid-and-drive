package com.carrefour.kata.bidanddrive.identity.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AppUserJpaRepository extends JpaRepository<AppUserEntity, Long> {

	List<AppUserEntity> findAllByOrderByIdAsc();

}
