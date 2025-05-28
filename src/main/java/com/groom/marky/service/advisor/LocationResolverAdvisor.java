package com.groom.marky.service.advisor;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import com.groom.marky.common.TmapGeocodingClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("사용자 요청의 location을 위경도로 변환하는 어드바이저")
	public class LocationResolverAdvisor implements CallAdvisor {

	private static final String LOCATION_KEY = "location";
	private final TmapGeocodingClient tmapClient;

	@Autowired
	public LocationResolverAdvisor(TmapGeocodingClient tmapClient) {
		this.tmapClient = tmapClient;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

		log.info("[LocationResolverAdvisor] 진입");

		String location = (String) request.context().get(LOCATION_KEY);


		if (location == null || location.isBlank()) {
			log.warn("location이 누락되어 LocationResolverAdvisor 스킵");
			return chain.nextCall(request);
		}

		Map<String, Double> coord = tmapClient.getLatLon(location);


		if (coord == null) {
			log.warn("tmapClient 결과가 null → location: {}", location);
			return chain.nextCall(request);
		}

		log.info("[LocationResolverAdvisor] location '{}' → lat={}, lon={}",
			location, coord.get("lat"), coord.get("lon"));

		//  여기만 mutate()로 변경
		ChatClientRequest modified = request.mutate()
			.context("lat", coord.get("lat"))
			.context("lon", coord.get("lon"))
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
