package com.groom.marky.service.tool;

import com.groom.marky.common.TmapTransitClient;
import com.groom.marky.domain.response.RouteDescriptionBuilder;
import com.groom.marky.domain.response.TmapRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubwayRouteSearchTool {

	private final TmapTransitClient tmapTransitClient;

	@Tool(
			name = "getRouteDetails",
			description = """
                    출발지(origin)와 목적지(destination)의 위도/경도를 기준으로
                    Tmap 기반 지하철 경유역 리스트를 조회합니다.
                    경유 역의 이름들이 리스트 형대로 반환됩니다.
                    """
	)

	public String getRouteDetails (
			@ToolParam(description = "출발지 위도", required = true) Double originLat,
			@ToolParam(description = "출발지 경도", required = true) Double originLon,
			@ToolParam(description = "목적지 위도", required = true) Double destLat,
			@ToolParam(description = "목적지 경도", required = true) Double destLon
	) {
		log.info("[getRouteDetails 호출] 출발지 위경도 : ({},{}), 목적지 위경도: ({},{})",
				originLat, originLon, destLat, destLon);

		if (originLat == null || originLon == null || destLat == null || destLon == null) {
			return "위도/경도 값이 부족합니다. 네 값 모두 제공해주세요.";
		}

		TmapRouteResponse response = tmapTransitClient.getRouteDetails(originLon, originLat, destLon, destLat);

		if (response == null) {
			return "지하철 경로를 찾을 수 없습니다.";
		}

		return RouteDescriptionBuilder.build(response);
	}
}
