package com.groom.marky.service.tool;

import java.util.List;
import java.util.stream.Collectors;

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
public class RestaurantSearchTool {

	private final VectorStore vectorStore;

	@Autowired
	public RestaurantSearchTool(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	@Tool(
		name = "restaurantSearch",
		description = """
			사용자가 원하는 분위기나 조건(mood)에 유사한 음식점을 location 기준으로 5개 추천합니다.
			메타데이터 내 formattedAddress 필드를 location에 기반해 필터링한 후, mood 임베딩 기반 유사도 검색을 수행합니다.
			"""
	)
	public List<Document> restaurantSearch(
		@ToolParam(description = "사용자 요구 분위기 리스트", required = true) List<String> mood,
		@ToolParam(description = "주소 필터링에 사용할 행정동, 역 등 위치", required = true) String location
	) {
		log.info("[restaurantSearch Tool 호출] location : {}, mood : {}", location, mood);

		if (location == null || mood == null || mood.isEmpty()) {
			log.warn("[restaurantSearch] mood 또는 location 누락됨");
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
