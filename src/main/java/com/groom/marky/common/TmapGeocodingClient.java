package com.groom.marky.common;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TmapGeocodingClient {

	private final RestTemplate restTemplate;

	@Autowired
	public TmapGeocodingClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Value("${TMAP_SECRET_KEY}")
	private String appKey;

	public Map<String, Double> getLatLon(String address) {
		String url = UriComponentsBuilder.fromHttpUrl("https://apis.openapi.sk.com/tmap/geo/fullAddrGeo")
			.queryParam("version", "1")
			.queryParam("coordType", "WGS84GEO")
			.queryParam("fullAddr", address)
			.toUriString();

		HttpHeaders headers = new HttpHeaders();
		headers.set("appKey", appKey);

		HttpEntity<Void> entity = new HttpEntity<>(headers);

		try {
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

			Map body = response.getBody();

			List coords = (List)((Map)body.get("coordinateInfo")).get("coordinate"); // 좌표

			if (coords.isEmpty()) {
				return null;
			}

			Map coord = (Map)coords.get(0);

			Double lat = Double.parseDouble((String)coord.get("lat"));
			Double lon = Double.parseDouble((String)coord.get("lon"));

		//	log.info("위경도 변환 완료. 위도 : {}, 경도 : {}", lat, lon);

			return Map.of(
				"lat", lat,
				"lon", lon
			);
		} catch (Exception e) {
			log.info(e.getMessage());
			return null;
		}
	}
}

