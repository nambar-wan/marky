package com.groom.marky.domain.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeName("circle")
public class Circle implements LocationRestriction {

	private Coordinate center;
	private double radius;

	@JsonCreator
	public Circle(
		@JsonProperty("center") Coordinate center,
		@JsonProperty("radius") double radius) {
		this.center = center;
		this.radius = radius;
	}

	public static Circle from(Rectangle box) {

		// 중간점 찾기
		double centerLat = (box.getSouth() + box.getNorth()) / 2.0;
		double centerLng = (box.getWest() + box.getEast()) / 2.0;
		Coordinate center = new Coordinate(centerLat, centerLng);

		// 반지름 구하기
		double cornerLat = box.getNorth();
		double cornerLng = box.getEast();
		int radius = (int)Math.ceil(
			haversineDistanceMeters(center.getLatitude(), center.getLongitude(), cornerLat, cornerLng));
		return new Circle(center, radius);
	}

	private static double haversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
		final double R = 6_371_000; // Earth radius in meters
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a =
			Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c;
	}

}
