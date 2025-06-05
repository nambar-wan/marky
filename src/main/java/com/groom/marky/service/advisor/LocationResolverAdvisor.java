package com.groom.marky.service.advisor;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import com.groom.marky.service.KakaoPlaceSearchService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("사용자 요청의 location을 위경도로 변환하는 어드바이저")
public class LocationResolverAdvisor implements CallAdvisor {

	private static final String LOCATION_KEY = "location";
	private static final String ORIGIN = "origin";
	private static final String DESTINATION = "destination";
	private static final String INTENT_KEY = "intent";

	private final KakaoPlaceSearchService kakaoPlaceSearchService;

	@Autowired
	public LocationResolverAdvisor(KakaoPlaceSearchService kakaoPlaceSearchService) {
		this.kakaoPlaceSearchService = kakaoPlaceSearchService;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

		log.info("[LocationResolverAdvisor] 진입");

		String intent = (String) request.context().get(INTENT_KEY);
		String location = (String) request.context().get(LOCATION_KEY);
		String origin = (String) request.context().get(ORIGIN);
		String destination = (String) request.context().get(DESTINATION);

		// 경로 처리
		if ("경로".equals(intent)) {

			if (origin == null || destination == null) {
				log.warn("[LocationResolverAdvisor] origin 또는 destination 이 null 입니다. 스킵합니다.");
				return chain.nextCall(request);
			}

			Map<String, Double> originCoord = kakaoPlaceSearchService.search(origin);
			Map<String, Double> destCoord = kakaoPlaceSearchService.search(destination);

			if (originCoord == null || destCoord == null) {
				log.warn("[LocationResolverAdvisor] 출발역 또는 도착역 위경도 검색 실패 - origin: {}, dest: {}", originCoord, destCoord);
				return chain.nextCall(request);
			}

			log.info("[LocationResolverAdvisor] 출발역 '{}' → lat={}, lon={}", origin, originCoord.get("lat"), originCoord.get("lon"));
			log.info("[LocationResolverAdvisor] 도착역 '{}' → lat={}, lon={}", destination, destCoord.get("lat"), destCoord.get("lon"));

			ChatClientRequest modified = request.mutate()
				.context("originLat", originCoord.get("lat"))
				.context("originLon", originCoord.get("lon"))
				.context("destLat", destCoord.get("lat"))
				.context("destLon", destCoord.get("lon"))
				.build();

			return chain.nextCall(modified);
		}

		// 일반 location 처리
		if (location == null || location.isBlank()) {
			log.warn("[LocationResolverAdvisor] location 이 누락되어 해당 어드바이저는 스킵합니다.");
			return chain.nextCall(request);
		}

		Map<String, Double> coordination = kakaoPlaceSearchService.search(location);

		if (coordination == null) {
			log.warn("[LocationResolverAdvisor] kakaoPlaceSearchService.search 결과가 null 입니다. location: {}", location);
			return chain.nextCall(request);
		}

		log.info("[LocationResolverAdvisor] location '{}' → lat={}, lon={}", location, coordination.get("lat"), coordination.get("lon"));

		ChatClientRequest modified = request.mutate()
			.context("lat", coordination.get("lat"))
			.context("lon", coordination.get("lon"))
			.build();

		return chain.nextCall(modified);
	}

	@Override
	public String getName() {
		return "LocationResolverAdvisor";
	}

	@Override
	public int getOrder() {
		return 2;
	}
}
