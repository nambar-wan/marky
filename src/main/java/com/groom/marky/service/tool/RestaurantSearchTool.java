package com.groom.marky.service.tool;

import java.util.List;
import java.util.stream.Collectors;

import com.groom.marky.common.RedisKeyParser;
import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.repository.RestaurantRepository;
import com.groom.marky.service.impl.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RestaurantSearchTool {

	private final VectorStore vectorStore;
	private final RedisService redisService;
	private final RestaurantRepository restaurantRepository;
	private double searchRadiusKm = 1;
	private double minRating = 3.5;

	@Tool(
			name = "searchRestaurant",
			description = """
			Redis의 places:restaurant의 값 중에서, 사용자의 요청 위치의 위/경도 값을 기준으로 일정 반경 내에 위치한 카페들의 아이디들을 리스트로 반환합니다.
			반환된 아이디들을 db에서 검색하여 db에 임베딩된 벡터 값을 이용하여 유사도 검색(similaritySearch)에 사용됩니다.
			"""
	)
	public List<String> searchRestaurant(
			@ToolParam(description = "사용자 목적지 위도값", required = true) Double lat,
			@ToolParam(description = "사용자 목적지 경도값", required = true) Double lon,
			@ToolParam(description = "사용자 요구 분위기 리스트", required = true) String mood
	) {
		log.info("[searchRestaurant Tool 호출] 위도 : {}, 경도 : {}, 디테일 : {}", lat, lon, mood);
		log.info("반경 {} km 내의 음식점을 탐색합니다.", searchRadiusKm);

		if (lat == null || lon == null || mood == null) {
			log.warn("[cafeSearch] 위도, 경도 혹은 검색 조건이 누락됨");
			return List.of();
		}

		String key = RedisKeyParser.getPlaceKey(GooglePlaceType.RESTAURANT);
		log.info("key : {}",key);
		List<String> nearbyPlacesId = redisService.getNearbyPlacesId(key, lat, lon, searchRadiusKm);
		log.info("근처 음식점 ID 수: {}", nearbyPlacesId.size());

		if(nearbyPlacesId.isEmpty()) return List.of();

		List<String> restaurantsWithReview = restaurantRepository.findByIdWhenReviewIsExist(nearbyPlacesId, minRating);
		log.info("리뷰가 있고 평점이 {} 이상인 근처 음식점 ID 수: {}", minRating, restaurantsWithReview.size());


		if(restaurantsWithReview.size() == 0) {
			log.info("주변에 리뷰가 있는 음식점이 없습니다. 리뷰가 없는 음식점을 포함하여 검색합니다.");
			return nearbyPlacesId;
		}

		double editedRating = minRating;
		List<String> prevList = restaurantsWithReview;
		while(restaurantsWithReview.size() > 50){
			prevList = restaurantsWithReview;
			editedRating += 0.2;
			if(editedRating >= 4.2) break;
			restaurantsWithReview = restaurantRepository.findByIdWhenReviewIsExist(nearbyPlacesId, editedRating);
			log.info("리뷰가 있고 평점이 {} 이상인 근처 음식점 ID 수: {}", editedRating, restaurantsWithReview.size());
		}
		restaurantsWithReview = prevList;

		return restaurantsWithReview;

	}

//	@Tool(
//		name = "searchRestaurant",
//		description = """
//		지정된 위치(location)를 기준으로 분위기(mood)에 유사한 음식점을 5개 추천합니다.
//		위치는 formattedAddress 메타데이터를 기준으로 필터링되며, pgvector 기반 유사도 계산을 통해 추천이 이루어집니다.
//	"""
//	)
//	public List<Document> restaurantSearch(
//		@ToolParam(description = "사용자 요구 분위기 리스트", required = true) String mood,
//		@ToolParam(description = "주소 필터링에 사용할 행정동, 역 등 위치", required = true) String location
//	) {
//		log.info("[restaurantSearch Tool 호출] location : {}, mood : {}", location, mood);
//
//		if (location == null || mood == null || mood.isEmpty()) {
//			log.warn("[restaurantSearch] mood 또는 location 누락됨");
//			return List.of();
//		}
//
//		String moodQuery = String.join(" ", mood);
//
//		return searchWithPostProcessing(moodQuery, location);
//	}

	private List<Document> searchWithPostProcessing(String moodQuery, String location) {
		try {
			// 후처리 필터링 방식 (가장 안전한 방법)
			log.info("후처리 필터링으로 검색");

			List<Document> allResults = vectorStore.similaritySearch(
				SearchRequest.builder()
					.query(moodQuery)
					.topK(100) // 충분한 결과를 가져온 후 필터링
					.similarityThreshold(0.6)
					.build()
			);

			if(allResults == null || allResults.isEmpty()) {
				log.warn("[restaurantSearch] allResults 없음");
			}
			return allResults.stream()
				.filter(doc -> {
					Object addressObj = doc.getMetadata().get("formattedAddress");
					if (addressObj instanceof String address) {
						return address.contains(location);
					}
					return false;
				})
				.limit(5)
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("후처리 필터링도 실패", e);
			return List.of();
		}
	}
}
