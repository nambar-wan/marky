package com.groom.marky.service.advisor;

import java.util.List;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import com.groom.marky.common.TmapTransitClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("출발지~목적지 사이의 지하철 경로를 조회하는 어드바이저")
public class SubwayRouteAdvisor implements CallAdvisor {

	private static final String DEPARTURE_LAT_KEY = "departureLat";
	private static final String DEPARTURE_LON_KEY = "departureLon";
	private static final String DESTINATION_LAT_KEY = "destinationLat";
	private static final String DESTINATION_LON_KEY = "destinationLon";

	private final TmapTransitClient tmapTransitClient;

	@Autowired
	public SubwayRouteAdvisor(TmapTransitClient tmapTransitClient) {
		this.tmapTransitClient = tmapTransitClient;
	}


	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

		log.info("SubwayRouteAdvisor 진입");

		Double departureLat = (Double)request.context().get(DEPARTURE_LAT_KEY);
		Double departureLon = (Double)request.context().get(DEPARTURE_LON_KEY);
		Double destinationLat = (Double)request.context().get(DESTINATION_LAT_KEY);
		Double destinationLon = (Double)request.context().get(DESTINATION_LON_KEY);

		// 위경도 누락 시 패스
		if (departureLat == null || departureLon == null || destinationLat == null || destinationLon == null) {
			log.warn("위경도 정보 누락 → SubwayRouteAdvisor 생략");
			return chain.nextCall(request);
		}

		// 역 이름들 조회
		List<String> stationList = tmapTransitClient.getSubwayStations(
			departureLon, departureLat, // startX, startY
			destinationLon, destinationLat // endX, endY
		);


		if (stationList.isEmpty()) {
			log.warn("지하철 경로 없음");
			return chain.nextCall(request);
		}

		ChatClientRequest modified = ChatClientRequest.builder()
			.prompt(request.prompt())
			.context(request.context())
			.context("subwayStations", stationList) // context에 역 목록 추가
			.build();

		log.info("지하철 경로 조회 완료: {}", stationList);
		return chain.nextCall(modified);
	}

	@Override
	public String getName() {
		return "SubwayRouteAdvisor";
	}

	@Override
	public int getOrder() {
		return 3;
	}
}
