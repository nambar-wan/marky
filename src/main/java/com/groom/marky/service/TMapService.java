package com.groom.marky.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.groom.marky.domain.response.TMapCongestionResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TMapService {

	private static final Logger log = LoggerFactory.getLogger(TMapService.class);
	@Value("${TMAP_SECRET_KEY}")
	private String appKey;

	private final RestTemplate restTemplate = new RestTemplate();

	public TMapCongestionResponse getSurroundingCongestion(double lat, double lng) {
		String url = UriComponentsBuilder
			.fromHttpUrl("https://apis.openapi.sk.com/tmap/puzzle/pois/around/congestion")
			.queryParam("lat", lat)
			.queryParam("lng", lng)
			.toUriString();

		log.info("url : {}", url);

		HttpHeaders headers = new HttpHeaders();
		headers.set("appKey", appKey); // 반드시 헤더에 포함
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		HttpEntity<Void> entity = new HttpEntity<>(headers);

		ResponseEntity<TMapCongestionResponse> response = restTemplate.exchange(
			url,
			HttpMethod.GET,
			entity,
			TMapCongestionResponse.class
		);

		return response.getBody();
	}
}
