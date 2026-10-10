package com.carrefour.kata.bidanddrive.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
class AppUserEntity {

	@Id
	private long id;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	protected AppUserEntity() {
	}

	long getId() {
		return id;
	}

	String getDisplayName() {
		return displayName;
	}

}
