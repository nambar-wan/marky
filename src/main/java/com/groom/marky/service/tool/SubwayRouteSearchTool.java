package com.groom.marky.service.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.groom.marky.common.TmapTransitClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubwayRouteSearchTool {

	private final TmapTransitClient tmapTransitClient;

	@Tool(
		name = "getSubwayStationList",
		description = """
				출발지(origin)와 목적지(destination)의 위도/경도를 기반으로,
				Tmap API를 이용해 지하철 경유역 목록을 조회합니다.
				반환값은 역 이름의 리스트입니다.
			"""
	)
	public String getSubwayStations(
		@ToolParam(description = "출발지 위도", required = true) Double originLat,
		@ToolParam(description = "출발지 경도", required = true) Double originLon,
		@ToolParam(description = "목적지 위도", required = true) Double destLat,
		@ToolParam(description = "목적지 경도", required = true) Double destLon
	) {
		log.info("[getSubwayStations 호출] 출발지 위경도 : ({},{}), 목적지 위경도: ({},{})",
			originLat, originLon, destLat, destLon);

		if (originLat == null || originLon == null || destLat == null || destLon == null) {
			return "위도/경도 값이 부족합니다. 네 값 모두 제공해주세요.";
		}

		List<String> stationList = tmapTransitClient.getSubwayStations(originLon, originLat, destLon, destLat);

		if (stationList == null || stationList.isEmpty()) {
			return "지하철 경로를 찾을 수 없습니다.";
		}

		return String.join(" -> ", stationList);
	}
}
