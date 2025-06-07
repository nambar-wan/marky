package com.groom.marky.service.tool;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.groom.marky.common.RedisKeyParser;
import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.repository.ActivityMetadataRepository;
import com.groom.marky.service.impl.RedisService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ActivitySearchTool {

	private final RedisService redisService;
	private final VectorStore vectorStore;
	private final ActivityMetadataRepository activityMetadataRepository;

	@Autowired
	public ActivitySearchTool(RedisService redisService, VectorStore vectorStore,
		ActivityMetadataRepository activityMetadataRepository) {
		this.redisService = redisService;
		this.vectorStore = vectorStore;
		this.activityMetadataRepository = activityMetadataRepository;
	}

	@Tool(
		name = "searchActivity",
		description = """
		사용자의 현재 위치(lat, lon)를 기준으로 반경 2km 이내에 위치한 특정 액티비티(activity_detail)의 ID 목록을 반환합니다.
		Redis Geo 데이터를 활용해 빠르게 인근 액티비티를 탐색하며, 결과는 유사도 검색(similaritySearch) 등에 활용됩니다.
	"""
	)
	public List<String> searchActivity(
		@ToolParam(description = "사용자 목적지 위도값", required = true) Double lat,
		@ToolParam(description = "사용자 목적지 경도값", required = true) Double lon,
		@ToolParam(description = "액티비티", required = true) String activity_detail

	) {

		log.info("[searchActivity Tool 호출] 위도 : {}, 경도 : {}, 디테일 : {}", lat, lon, activity_detail);
		List<String> intentPlace = new ArrayList<>();
		String key = RedisKeyParser.getPlaceKey(GooglePlaceType.ACTIVITY);
		log.info("key : {}", key);
		List<String> nearbyPlacesId = redisService.getNearbyPlacesId(key, lat, lon, 2);
		log.info("근처 장소 ID 목록: {}", nearbyPlacesId);
		log.info("근처 장소 ID 수: {}", nearbyPlacesId.size());

		List<String> byActivityTypeAndPlaceIds = activityMetadataRepository.findByActivityTypeAndPlaceIds(
			activity_detail, nearbyPlacesId);
		log.info("byActivityTypeAndPlaceIds 필터링된 Document : {}, 수: {}", byActivityTypeAndPlaceIds,
			byActivityTypeAndPlaceIds.size());
		return byActivityTypeAndPlaceIds;
	}

}
