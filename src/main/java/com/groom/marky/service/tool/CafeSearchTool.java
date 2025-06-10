package com.groom.marky.service.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.groom.marky.common.RedisKeyParser;
import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.repository.CafeRepository;
import com.groom.marky.service.impl.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CafeSearchTool {

	private final RedisService redisService;
	private final CafeRepository cafeRepository;
	private double searchRadiusKm = 0.5;
	private double minRating = 4.0;

	@Tool(
		name = "searchCafe",
		description = """
			Redis의 places:cafe의 값 중에서, 사용자의 요청 위치의 위/경도 값을 기준으로 일정 반경 내에 위치한 카페들의 아이디들을 리스트로 반환합니다.
			반환된 아이디들을 db에서 검색하여 db에 임베딩된 벡터 값을 이용하여 유사도 검색(similaritySearch)에 사용됩니다.
			"""
	)
	public List<String> searchCafe(
		@ToolParam(description = "사용자 목적지 위도값", required = true) Double lat,
		@ToolParam(description = "사용자 목적지 경도값", required = true) Double lon,
		@ToolParam(description = "사용자 요구 분위기 리스트", required = true) String mood
	) {
		log.info("[searchCafe Tool 호출] 위도 : {}, 경도 : {}, 디테일 : {}", lat, lon, mood);
		log.info("반경 {} km 내의 카페를 탐색합니다.", searchRadiusKm);

		if (lat == null || lon == null || mood == null) {
			log.warn("[cafeSearch] 위도, 경도 혹은 검색 조건이 누락됨");
			return List.of();
		}

		String key = RedisKeyParser.getPlaceKey(GooglePlaceType.CAFE);
		log.info("key : {}", key);
		List<String> nearbyPlacesId =
				redisService.getNearbyPlacesId(key, lat, lon, searchRadiusKm);
		log.info("근처 카페 ID 수: {}", nearbyPlacesId.size());

		if (nearbyPlacesId.isEmpty())
			return List.of();


		List<String> cafesWithReview =
				cafeRepository.findByIdWhenReviewIsExist(nearbyPlacesId, minRating);
		log.info("리뷰가 있고 평점이 {} 이상인 근처 카페 ID 수: {}", minRating, cafesWithReview.size());

		if (cafesWithReview.size() == 0) {
			log.info("주변에 리뷰가 있는 카페가 없습니다. 리뷰가 없는 카페를 포함하여 검색합니다.");
			return nearbyPlacesId;
		}

		double editedRating = minRating;
		List<String> prevList = cafesWithReview;
		while(cafesWithReview.size() > 50){
			prevList = cafesWithReview;
			editedRating += 0.2;
			if(editedRating >= 4.2) break;
			cafesWithReview = cafeRepository.findByIdWhenReviewIsExist(nearbyPlacesId, editedRating);
			log.info("리뷰가 있고 평점이 {} 이상인 근처 음식점 ID 수: {}", editedRating, cafesWithReview.size());
		}
		cafesWithReview = prevList;


		return cafesWithReview;
	}

}
