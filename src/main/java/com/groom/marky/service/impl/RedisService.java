package com.groom.marky.service.impl;

import static com.groom.marky.common.constant.MetadataKeys.*;
import static com.groom.marky.domain.response.GooglePlacesApiResponse.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.marky.common.RedisKeyParser;
import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.domain.request.Rectangle;
import com.groom.marky.domain.response.RefreshTokenInfo;
import com.groom.marky.domain.response.GooglePlacesApiResponse;

@Slf4j
@Service
public class RedisService {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private static final String RESTAURANT_RECT_ALL_KEY = "place:restaurant:rects:all";
	private static final String RESTAURANT_RECT_PROCESSED_KEY = "place:restaurant:rects:processed";
	private static final String OVER_LENGTH_PLACE_KEY = "place:overlength";

	@Autowired
	public RedisService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	public void setPlacesLocation(GooglePlaceType type, GooglePlacesApiResponse response) {
		String key = RedisKeyParser.getPlaceKey(type);
		List<Place> places = response.places();

		for (Place place : places) {
			redisTemplate.opsForGeo()
				.add(key, new Point(place.location().longitude(), place.location().latitude()), place.id());
		}
	}
	public void setSeoulPlacesLocation(GooglePlaceType type, List<Document> documents) {
		String key = RedisKeyParser.getPlaceKey(type);

		for (Document document : documents) {
			Map<String, Object> metadata = document.getMetadata();
			String lat = (String) metadata.get(LAT);
			String lon = (String) metadata.get(LON);
			String placeId = (String) metadata.get(GOOGLEPLACEID);
			log.info("lat : {}, lon : {}, placeId : {}",lat,lon,placeId);
			if (lat != null && lon != null && placeId != null) {
				redisTemplate.opsForGeo()
						.add(key, new Point(Double.parseDouble(lon), Double.parseDouble(lat)), placeId);
			}
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

	public void markRectangleAsProcessed(Rectangle rect) {
		redisTemplate.opsForSet().add(RESTAURANT_RECT_PROCESSED_KEY, rect.toString());
	}

	public void markPlaceAsOverLength(Place place) {
		redisTemplate.opsForSet().add(OVER_LENGTH_PLACE_KEY, place.id());
	}

	public void saveAllRects(Set<Rectangle> rects) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(rects);
		redisTemplate.opsForValue().set(RESTAURANT_RECT_ALL_KEY, json);
	}

	public Set<Rectangle> loadAllRects() throws JsonProcessingException {
		String json = redisTemplate.opsForValue().get(RESTAURANT_RECT_ALL_KEY);
		if (json == null) return Set.of();

		ObjectMapper mapper = new ObjectMapper();
		Set<Rectangle> allRects = mapper.readValue(json, new TypeReference<Set<Rectangle>>() {});

		// 처리된 rect들
		Set<String> processedRectStrings = Optional.ofNullable(
			redisTemplate.opsForSet().members(RESTAURANT_RECT_PROCESSED_KEY)).orElse(Collections.emptySet());

		// 아직 처리되지 않은 것만 필터링
		return allRects.stream()
			.filter(rect -> !processedRectStrings.contains(rect.toString()))
			.collect(Collectors.toSet());
	}

	public void setRefreshToken(RefreshTokenInfo tokenInfo) throws JsonProcessingException {

		String userEmail = tokenInfo.getUserEmail();
		long expirationMillis = tokenInfo.getExpiresAt() - System.currentTimeMillis();
		String key = RedisKeyParser.getRefreshTokenKey(userEmail);
		String value = objectMapper.writeValueAsString(tokenInfo);

		redisTemplate.opsForValue().set(key, value, expirationMillis, TimeUnit.MILLISECONDS);

	}

	// 리프레쉬 토큰 삭제 및 액세스 토큰 블랙리스트 등록
	public void deleteRefreshToken(RefreshTokenInfo tokenInfo) {

		String userEmail = tokenInfo.getUserEmail();
		String refreshTokenKey = RedisKeyParser.getRefreshTokenKey(userEmail);

		// 리프레쉬 토큰 삭제
		redisTemplate.delete(refreshTokenKey);

	}

	public void registerBlacklist(String accessToken, long expiresAt) {

		long expirationMillis = expiresAt - System.currentTimeMillis();

		if (expirationMillis <= 0) {
			log.warn("만료된 access token을 블랙리스트에 등록할 수 없습니다.");
			return;
		}

		String blacklistKey = RedisKeyParser.getBlacklistKey(accessToken);

		redisTemplate.opsForValue().set(blacklistKey,"logout", expirationMillis, TimeUnit.MILLISECONDS);
	}

	public boolean isInBlacklist(String accessToken) {

		String key = RedisKeyParser.getBlacklistKey(accessToken);

		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	public RefreshTokenInfo getRefreshToken(String userEmail) throws JsonProcessingException {

		String key = RedisKeyParser.getRefreshTokenKey(userEmail);
		String savedRefreshTokenInfo = redisTemplate.opsForValue().get(key);

		if (savedRefreshTokenInfo == null) {
			throw new IllegalArgumentException("해당 사용자의 리프레쉬 토큰이 레디스 내부에 존재하지 않습니다.");
		}

		return objectMapper.readValue(savedRefreshTokenInfo, RefreshTokenInfo.class);

	}

	public void deleteRefreshTokenByKey(String userEmail) {
		String key = RedisKeyParser.getRefreshTokenKey(userEmail);
		redisTemplate.delete(key);
	}
}
