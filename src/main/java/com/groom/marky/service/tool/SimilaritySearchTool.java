package com.groom.marky.service.tool;

import static com.groom.marky.common.constant.MetadataKeys.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SimilaritySearchTool {

	private final VectorStore vectorStore;

	@Autowired
	public SimilaritySearchTool(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

//	@Tool(
//		name = "similaritySearch",
//		description = """
//		지정된 장소 ID 리스트(ids)를 기반으로 분위기(mood)와 의미적으로 유사한 장소를 5개 추천합니다.
//		pgvector 기반 벡터 임베딩을 활용하여 mood와 장소 설명 간의 유사도를 계산합니다.
//	""",
//			resultConverter = CustomToolCallResultConverter.class
//	)
	public List<Document> similaritySearch(
		@ToolParam(description = "사용자가 원하는 분위기", required = true) String mood,
		@ToolParam(description = "지정된 장소 리스트. 해당 아이디로 벡터 데이터베이스 메타데이터 조회. 대상 선정", required = true) List<String> ids
	) {
		log.info("[similaritySearch Tool 호출] mood : {}, ids : {}", mood, ids.size());

		if (mood == null || ids.isEmpty()) {
			log.warn("[PlaceVectorSearchTool:similaritySearch] 정보가 부족합니다.");
			return List.of();
		}
		log.info("Similarity Search Tool에서 받는 리스트 : {}", ids);

		FilterExpressionBuilder b = new FilterExpressionBuilder();
		FilterExpressionBuilder.Op op = null;


		double startSearchingIds = System.currentTimeMillis();
		log.info("Start searching IDs in Database");

		// 메타데이터에서 구글 플레이스 ID 검색
		for (String id : ids) {
			if (op == null) {
				op = b.eq(GOOGLEPLACEID, id);
				// 여기서 메타데이터에 저장된 위경도 데이터 파악..
				// {"lat": 37.5369564, "lon": 127.0491856,
			} else {
				op = b.or(op, b.eq(GOOGLEPLACEID, id));
			}
		}

		double endSearchingIds = System.currentTimeMillis();
		log.info("Finish searching IDs in Database");
		log.info("Searching IDs time: {} ms", endSearchingIds - startSearchingIds);

		double startCalculateSimilarity = System.currentTimeMillis();
		log.info("Start similarity calculation");
		// 리필터?
		List<Document> result =  vectorStore.similaritySearch(
			SearchRequest.builder()
				.query(mood)
				.topK(5)
				.similarityThreshold(0.2)
				.filterExpression(op.build())
				.build());

		double endCalculateSimilarity = System.currentTimeMillis();
		log.info("Finish similarity calculation");
		log.info("Similarity calculation time: {} ms", endCalculateSimilarity - startCalculateSimilarity);


//		return resultConverter(result);
		return result;
	}

	List<String> resultConverter(List<Document> result) {
//		**최종 응답 출력 규칙**
//		- 아래 JSON 형태로만 출력해야 함 (절대 설명하지 마)
//		```json ``` 으로 감싸지 마.
//				- ChatResponse 형식:
//		{
//			"message": "사용자에게 보여줄 요약 메시지",
//				"places": [
//			{
//				"name": "...",
//					"address": "...",
//					"latitude": 0.0,
//					"longitude": 0.0,
//					"rating": 0.0,
//					"reviewCount": 0,
//					"reviewSummary": "..."
//			}
//		  ]
//		}
		List<String> resultList = new ArrayList<>();


		for(Document store: result) {
			StringBuilder sb = new StringBuilder();
			sb.append("name:" + store.getMetadata().get("displayName") + "\n");
			sb.append("address:" + store.getMetadata().get("formattedAddress") + "\n");
			sb.append("lat:" + store.getMetadata().get("lat") + "\n");
			sb.append("lon:" + store.getMetadata().get("lon") + "\n");
			sb.append("rating:" + store.getMetadata().get("rating") + "\n");
			sb.append("reviewCount:" + store.getMetadata().get("userRatingCount") + "\n\n");
			resultList.add(sb.toString());
		}

		return resultList;

	}

}
