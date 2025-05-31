package com.groom.marky.service.advisor;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import com.groom.marky.common.TmapGeocodingClient;
import com.groom.marky.service.KakaoPlaceSearchService;
import com.groom.marky.service.impl.KakaoPlaceSearchServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("사용자 요청의 location을 위경도로 변환하는 어드바이저")
	public class LocationResolverAdvisor implements CallAdvisor {

	private static final String LOCATION_KEY = "location";
	private final KakaoPlaceSearchService kakaoPlaceSearchService;

	@Autowired
	public LocationResolverAdvisor(KakaoPlaceSearchService kakaoPlaceSearchService) {
		this.kakaoPlaceSearchService = kakaoPlaceSearchService;
	}


	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

		log.info("[LocationResolverAdvisor] 진입");

		String location = (String) request.context().get(LOCATION_KEY);


		if (location == null || location.isBlank()) {
			log.warn("[LocationResolverAdvisor] location 이 누락되어 해당 어드바이저는 스킵합니다.");
			return chain.nextCall(request);
		}

		Map<String, Double> coordination = kakaoPlaceSearchService.search(location);


		if (coordination == null) {
			log.warn("[LocationResolverAdvisor] kakaoPlaceSearchService.search 결과가 null 입니다. 해당 어드바이저는 스킵합니다. location: {}", location);
			return chain.nextCall(request);
		}

		log.info("[LocationResolverAdvisor] location '{}' → lat={}, lon={}", location, coordination.get("lat"), coordination.get("lon"));

		//  여기만 mutate()로 변경
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
