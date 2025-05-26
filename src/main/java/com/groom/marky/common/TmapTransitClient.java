package com.groom.marky.common;

import static java.util.Map.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
			String url = "https://apis.openapi.sk.com/transit/routes";

			// 요청 바디 구성
			Map<String, Object> body = of(
				"startX", String.valueOf(startX),
				"startY", String.valueOf(startY),
				"endX", String.valueOf(endX),
				"endY", String.valueOf(endY),
				"lang", 0,
				"format", "json",
				"count", 10
			);

			// 헤더 구성
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("accept", "application/json");
			headers.set("appKey", appKey);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			ResponseEntity<String> response = restTemplate.exchange(
				url, HttpMethod.POST, request, String.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				log.warn("TMAP Transit API 호출 실패: {}", response.getStatusCode());
				return List.of();
			}

			JsonNode root = objectMapper.readTree(response.getBody());
			List<String> stationList = new ArrayList<>();

			for (JsonNode path : root.path("metaData").path("plan").path("itineraries")) {
				for (JsonNode leg : path.path("legs")) {
					if ("SUBWAY".equalsIgnoreCase(leg.path("mode").asText())) {
						for (JsonNode stop : leg.path("passStopList")) {
							String name = stop.path("stationName").asText();
							if (!name.isBlank()) {
								stationList.add(name);
							}
						}
					}
				}
			}

			return stationList;

		} catch (Exception e) {
			log.error("TMAP Transit API 처리 중 오류 발생", e);
			return List.of();
		}
	}
}
