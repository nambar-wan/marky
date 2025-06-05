package com.groom.marky.domain.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GoogleUserInfo {
	@JsonProperty(value = "sub")
	private String googleId;
	private String name;

	@JsonProperty(value = "email")
	private String userEmail;
}
