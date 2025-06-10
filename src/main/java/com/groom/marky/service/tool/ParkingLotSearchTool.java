package com.groom.marky.service.tool;

import java.util.List;

import org.springframework.ai.document.Document;
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
public class ParkingLotSearchTool {

	private final RedisService redisService;
	private final SimilaritySearchTool similaritySearchTool;

	@Autowired
	public ParkingLotSearchTool(RedisService redisService, SimilaritySearchTool similaritySearchTool) {
		this.redisService = redisService;
		this.similaritySearchTool = similaritySearchTool;
	}

	@Tool(
		name = "searchParkingLots",
		description = """
		사용자의 현재 위치(lat, lon)를 기준으로 반경 1km 이내의 주차장 ID 목록을 반환합니다.
		Redis 기반 Geo 데이터를 활용하여 빠르고 효율적으로 근처 주차장을 찾습니다.
		반환된 ID는 이후 similaritySearch에서 유사 장소 추천 등에 사용됩니다.
	"""
	)
	public List<Document> searchParkingLots(
		@ToolParam(description = "사용자 목적지 위도값", required = true) Double lat,
		@ToolParam(description = "사용자 목적지 경도값", required = true) Double lon,
		@ToolParam(description = "사용자 요구 분위기 리스트", required = true) String mood
	) {

		log.info("[searchParkingLots Tool 호출] 위도 : {}, 경도 : {}", lat, lon);
		log.info("사용자 요구 사항 : {}", mood);

		String key = RedisKeyParser.getPlaceKey(GooglePlaceType.PARKING);
		// place:parking

		// 필요한건?? 근처 주차장 조회다..
		List<String> nearbyPlacesId = redisService.getNearbyPlacesId(key, lat, lon, 1);

		return similaritySearchTool.similaritySearch(mood, nearbyPlacesId);

	}

}
