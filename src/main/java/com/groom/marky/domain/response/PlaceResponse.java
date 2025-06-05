package com.groom.marky.domain.response;

import lombok.Data;

@Data
public class PlaceResponse {

	private String name;
	private String address;
	private double latitude;
	private double longitude;
	private double rating;
	private int reviewCount;
	private String reviewSummary;
}
