package com.groom.marky.domain;

public enum Role {

	ROLE_USER("유저"),
	ROLE_ADMIN("어드민");

	private final String description;

	Role(String description) {
		this.description = description;
	}
}
