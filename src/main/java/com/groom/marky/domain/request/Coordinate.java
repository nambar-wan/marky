package com.groom.marky.domain.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Coordinate {
	private double latitude;
	private double longitude;

	@JsonCreator
	public Coordinate(
		@JsonProperty("latitude") double latitude,
		@JsonProperty("longitude") double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}



}

