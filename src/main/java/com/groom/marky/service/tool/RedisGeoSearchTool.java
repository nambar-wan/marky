package com.groom.marky.service.tool;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.groom.marky.common.RedisKeyParser;
import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.service.impl.RedisService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisGeoSearchTool {

	private final RedisService redisService;

	@Autowired
	public RedisGeoSearchTool(RedisService redisService) {
		this.redisService = redisService;
	}

	@Tool(
		name = "searchParkingLots",
		description = """
			사용자의 현재 위치(lat, lon)를 기준으로 반경 2km 이내에 위치한 주차장들의 고유 ID 목록을 반환합니다.
			이 도구는 Redis 기반의 위치(Geo) 데이터를 활용하여 빠르고 효율적으로 근처 주차장을 찾습니다.
			반환된 ID 목록은 이후 벡터 검색(similaritySearch) 등의 추가 처리에 사용될 수 있습니다.
			"""
	)
	public List<String> searchParkingLots(
		@ToolParam(description = "사용자 목적지 위도값", required = true) Double lat,
		@ToolParam(description = "사용자 목적지 경도값", required = true) Double lon
	) {

		log.info("[searchParkingLots Tool 호출] 위도 : {}, 경도 : {}", lat, lon);

		String key = RedisKeyParser.getPlaceKey(GooglePlaceType.PARKING);

		// 필요한건?? 근처 주차장 조회다..

		return redisService.getNearbyPlacesId(key, lat, lon, 2);

	}

}
