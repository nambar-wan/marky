package com.groom.marky.service.tool;

import com.groom.marky.common.RedisKeyParser;
import com.groom.marky.common.constant.GooglePlaceType;
import com.groom.marky.service.impl.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ActivitySearchTool {

    private final RedisService redisService;
    private final VectorStore vectorStore;

    @Autowired
    public ActivitySearchTool(RedisService redisService, VectorStore vectorStore) {
        this.redisService = redisService;
        this.vectorStore = vectorStore;
    }

    @Tool(
            name = "searchActivity",
            description = """
			사용자의 현재 위치(lat, lon)를 기준으로 반경 2km 이내에 위치한 intent들의 고유 ID 목록을 반환합니다.
			이 도구는 Redis 기반의 위치(Geo) 데이터를 활용하여 빠르고 효율적으로 근처 주차장을 찾습니다.
			반환된 ID 목록은 이후 벡터 검색(similaritySearch) 등의 추가 처리에 사용될 수 있습니다.
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
        log.info("key : {}",key);
        List<String> nearbyPlacesId = redisService.getNearbyPlacesId(key, lat, lon, 2);
        log.info("근처 장소 ID 목록: {}", nearbyPlacesId);

        Filter.Expression idFilter = new Filter.Expression(
                Filter.ExpressionType.IN,
                new Filter.Key("googlePlaceId"),
                new Filter.Value(nearbyPlacesId)  // List<String>을 직접 전달
        );

        // activity_type도 함께 필터링
        Filter.Expression activityFilter = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("activity_type"),
                new Filter.Value(activity_detail)
        );

        // AND 조건으로 결합
        Filter.Expression combinedFilter = new Filter.Expression(
                Filter.ExpressionType.AND,
                idFilter,
                activityFilter
        );
        log.info("벡터 필터 조건: {}", combinedFilter);

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .filterExpression(combinedFilter)
                        .build()
        );
        log.info("검색된 Document 수: {}, 내용: {}", docs.size(), docs);

        return docs.stream()
                .map(doc -> (String) doc.getMetadata().get("googlePlaceId"))
                .filter(id -> id != null) // null 체크
                .collect(Collectors.toList());
    }

}
