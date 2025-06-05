package com.groom.marky.common;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.groom.marky.domain.response.TmapRouteResponse;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TmapTransitClient {

	private static final int LANG = 0;
	private static final String FORMAT = "json";
	private static final int COUNT = 10;
	private static final String URL = "https://apis.openapi.sk.com/transit/routes";

	@Value("${TMAP_SECRET_KEY}")
	private String appKey;

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	@Autowired
	public TmapTransitClient(ObjectMapper objectMapper, RestTemplate restTemplate) {
		this.objectMapper = objectMapper;
		this.restTemplate = restTemplate;
	}

	public List<String> getSubwayStations(double startX, double startY, double endX, double endY) {
		try {
			HttpEntity<Map<String, Object>> requestEntity = buildRequest(startX, startY, endX, endY);
			ResponseEntity<String> response = restTemplate.exchange(URL, HttpMethod.POST, requestEntity, String.class);

			return parseStationNames(response.getBody());

		} catch (Exception e) {
			log.error("TMAP Transit API 처리 중 오류 발생", e);
			return Collections.emptyList();
		}
	}

	public TmapRouteResponse getRouteDetails(double startX, double startY, double endX, double endY) {
		try {
			HttpEntity<Map<String,Object>> requestEntity = buildRequest(startX,startY,endX,endY);
			ResponseEntity<String> response = restTemplate.exchange(URL, HttpMethod.POST, requestEntity, String.class);
			return parseFullRoute(response.getBody());
		} catch (Exception e) {
			log.error("TMAP Transit API 전체 응답 파싱 . 오류 발생", e);
			return null;
		}
	}

	private HttpEntity<Map<String, Object>> buildRequest(double startX, double startY, double endX, double endY) {
		Map<String, Object> body = Map.of(
				"startX", startX,
				"startY", startY,
				"endX", endX,
				"endY", endY,
				"lang", LANG,
				"format", FORMAT,
				"count", COUNT
		);

		// 헤더 구성
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("accept", "application/json");
		headers.set("appKey", appKey);

		return new HttpEntity<>(body, headers);

	}

	// 기존 역 리스트만 파싱하는 메서드
	private List<String> parseStationNames(String responseBody) throws Exception {
		JsonNode root = objectMapper.readTree(responseBody);
		JsonNode itinerariesNode = root.path("metaData").path("plan").path("itineraries");

		if (itinerariesNode.isMissingNode() || !itinerariesNode.isArray() || itinerariesNode.isEmpty()) {
			log.warn("응답에 경로 정보가 없습니다.");
			return Collections.emptyList();
		}

		TmapRouteResponse tmapRouteResponse = objectMapper.treeToValue(itinerariesNode.get(0), TmapRouteResponse.class);
		List<String> stationList = new ArrayList<>();

		for (TmapRouteResponse.LegDto leg : tmapRouteResponse.getLegs()) {
			if (leg.getPassStopList() != null && leg.getPassStopList().getStationList() != null) {
				for (TmapRouteResponse.StationDto station : leg.getPassStopList().getStationList()) {
					String name = station.getStationName();
					if (name != null && !name.isBlank()) {
						stationList.add(name);
					}
				}
			}
		}
		return stationList;
	}

	// 전체 경로 객체를 파싱하는 메서드
	private TmapRouteResponse parseFullRoute(String responseBody) throws Exception {
		JsonNode root = objectMapper.readTree(responseBody);
		JsonNode itinerariesNode = root.path("metaData").path("plan").path("itineraries");

		if (itinerariesNode.isMissingNode() || !itinerariesNode.isArray() || itinerariesNode.isEmpty()) {
			log.warn("응답에 전체 경로 정보가 없습니다.");
			return null;
		}

		return objectMapper.treeToValue(itinerariesNode.get(0), TmapRouteResponse.class);
	}
}
