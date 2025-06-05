package com.groom.marky.domain;

public enum LoginType {
	LOCAL("Local Login"),
	GOOGLE("Google OAuth Login");

	private final String description;

	LoginType(String description) {
		this.description = description;
	}
}
