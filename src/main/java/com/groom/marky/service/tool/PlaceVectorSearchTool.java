package com.groom.marky.service.tool;

import static com.groom.marky.common.constant.MetadataKeys.*;

import java.util.List;
import java.util.Map;

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
public class PlaceVectorSearchTool {

	private final VectorStore vectorStore;

	@Autowired
	public PlaceVectorSearchTool(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	@Tool(
		name = "similaritySearch",
		description = """
			지정된 장소 ID 리스트(ids)를 기반으로 분위기(mood)와 의미적으로 유사한 장소를 5개 추천합니다.
			이 도구는 pgvector 기반의 벡터 데이터베이스를 사용하여 장소 설명과 mood 간의 유사도를 계산합니다.
			"""
	)
	public List<Document> similaritySearch(
		@ToolParam(description = "사용자가 원하는 분위기", required = true) String mood,
		@ToolParam(description = "지정된 장소 리스트. 해당 아이디로 벡터 데이터베이스 메타데이터 조회. 대상 선정", required = true) List<String> ids) {
		log.info("[similaritySearch Tool 호출] mood : {}, ids : {}", mood, ids.size());


		if (mood == null || ids.isEmpty()) {
			log.warn("[PlaceVectorSearchTool:similaritySearch] 정보가 부족합니다.");
			return List.of();
		}

		FilterExpressionBuilder b = new FilterExpressionBuilder();
		FilterExpressionBuilder.Op op = null;


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


		// 리필터?
		return vectorStore.similaritySearch(
			SearchRequest.builder()
				.query(mood)
				.topK(3)
				.similarityThreshold(0.6)
				.filterExpression(op.build())
				.build());

	}

}
