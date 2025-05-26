package com.groom.marky.domain.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Coordinate {
	private double latitude;
	private double longitude;

	public Coordinate(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}



}

