package com.groom.marky.service.impl;

import static com.groom.marky.domain.response.GooglePlacesApiResponse.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import com.groom.marky.common.RedisKeyParser;
import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.domain.response.GooglePlacesApiResponse;

@Service
public class RedisService {

	private final StringRedisTemplate redisTemplate;

	@Autowired
	public RedisService(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void setPlacesLocation(GooglePlaceType type, GooglePlacesApiResponse response) {
		String key = RedisKeyParser.getPlaceKey(type);
		List<Place> places = response.places();

		for (Place place : places) {
			redisTemplate.opsForGeo()
				.add(key, new Point(place.location().longitude(), place.location().latitude()), place.id());
		}
	}

	public List<String> getNearbyPlacesId(String key, double lat, double lon, double radiusKm) {
		GeoResults<RedisGeoCommands.GeoLocation<String>> results =
			redisTemplate.opsForGeo().radius(
				key,
				new Circle(new Point(lon, lat), new Distance(radiusKm, Metrics.KILOMETERS))
			);

		if (results == null) {
			return List.of();
		}

		return results.getContent().stream()
			.map(GeoResult::getContent)
			.map(RedisGeoCommands.GeoLocation::getName)
			.toList();
	}
}
