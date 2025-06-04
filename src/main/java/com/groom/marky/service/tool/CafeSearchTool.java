package com.groom.marky.service.tool;

import com.groom.marky.service.impl.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CafeSearchTool {

	private final VectorStore vectorStore;
	private final RedisService redisService;


	@Tool(
		name = "cafeSearch",
		description = """
			Redis의 places:cafe의 값 중에서, 사용자의 요청 위치의 위/경도 값을 기준으로 일정 반경 내에 위치한 카페들의 아이디들을 리스트로 반환합니다.
			반환된 아이디들을 db에서 검색하여 db에 임베딩된 벡터 값을 이용하여 유사도 검색(similaritySearch)에 사용됩니다.
			"""
	)
	public List<Document> cafeSearch(
			@ToolParam(description = "사용자 목적지 위도값", required = true) Double lat,
			@ToolParam(description = "사용자 목적지 경도값", required = true) Double lon,
			@ToolParam(description = "사용자 요구 분위기 리스트", required = true) String mood
	) {
		log.info("[searchActivity Tool 호출] 위도 : {}, 경도 : {}, 디테일 : {}", lat, lon, mood);

		if (lat == null || lon == null || mood == null) {
			log.warn("[cafeSearch] 위도, 경도 혹은 검색 조건이 누락됨");
			return List.of();
		}

		String moodQuery = String.join(" ", mood);

		return searchWithPostProcessing(moodQuery, location);
	}

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
