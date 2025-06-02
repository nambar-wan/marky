package com.groom.marky.service.tool;

import com.groom.marky.domain.response.SubwayRouteDescriptionBuilder;
import com.groom.marky.domain.response.SubwayRouteResponse;
import com.groom.marky.service.SubwayRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubwayRouteSearchTool {

    @Autowired
    private SubwayRouteService subwayRouteService;

    @Tool(
            name ="getSubwayRoute",
            description = """
                    출발역(origin)과 도착역(destination)을 기준으로
                    서울 지하철의 최적 경로(최단 소요 시간)를 조회합니다.
                    총 소요 시간, 경유역 목록, 환승 정보 등을 포함한 경로를 반환합니다.
                    """
    )
    public String getSubwayRoute(
            @ToolParam(description = "출발역 이름", required = true) String origin,
            @ToolParam(description = "도착역 이름", required = true) String destination
    ) {
        log.info("[getSubwayRoute 호출] 출발역: {}, 도착역: {}", origin, destination);
        SubwayRouteResponse.SubwayRouteDto route = subwayRouteService.findShortestRoute(origin, destination);
        return SubwayRouteDescriptionBuilder.build(route);
    }
}
