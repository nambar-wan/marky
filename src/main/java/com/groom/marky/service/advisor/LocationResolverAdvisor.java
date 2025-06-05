package com.groom.marky.service.advisor;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import com.groom.marky.service.KakaoPlaceSearchService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("사용자 요청의 location을 위경도로 변환하는 어드바이저")
	public class LocationResolverAdvisor implements CallAdvisor {

	private static final String LOCATION_KEY = "location";
	private final KakaoPlaceSearchService kakaoPlaceSearchService;
	private final ChatModel chatModel;
	private final ObjectMapper objectMapper;

	private static final String ORIGIN = "origin";
	private static final String DESTINATION = "destination";
	private static final String CATEGORY_CODE = "category_code";


	@Autowired
	public LocationResolverAdvisor(KakaoPlaceSearchService kakaoPlaceSearchService, ChatModel chatModel, ObjectMapper objectMapper) {
		this.kakaoPlaceSearchService = kakaoPlaceSearchService;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }


	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

		log.info("[LocationResolverAdvisor] 진입");

		String intent = (String) request.context().get("intent"); // **
		String location = (String) request.context().get(LOCATION_KEY);
		Prompt prompt = new Prompt(List.of(
				new SystemMessage("""
						너는 location 문자열을 입력받아, 해당 내용을 기반으로 KakaoMapCategoryGroupCode 중 하나를 정확하게 선택하는 역할을 수행한다.
						
						다음 조건을 반드시 지켜서 **정확히 JSON 객체 하나만 반환해라**:
						- 코드블록, 마크다운 (` ``` ` 등)을 절대로 사용하지 마
						- key는 `"category_code"`로 고정해라
						- value는 아래 18개 enum 코드 중 하나여야 하며, 절대 다른 값이나 설명을 추가하지 마
						- location이 비어있거나 매핑할 수 없다면 `"null"`로 처리해라
						
						**단, 아래의 경우 반드시 "PO3"으로 매핑할 것:**
						- 지역명 또는 행정구 명칭이 들어온 경우 (예: "영등포", "여의도", "용산", "종로", "강남", "부산", "제주" 등)
						- 이 경우 장소나 업종이 아니라 **지리적 지역**이나 행정구역 단위로 보이면 무조건 `"PO3"`로 반환해라
						
						[출력 형식 예시]
						{ "category_code": "CE7" }
						
						[매핑 대상 KakaoMapCategoryGroupCode 목록 (총 18개)]
						
						MT1 : 대형마트
						CS2 : 편의점
						PS3 : 어린이집, 유치원
						SC4 : 학교
						AC5 : 학원
						PK6 : 주차장
						OL7 : 주유소, 충전소
						SW8 : 지하철역
						BK9 : 은행
						CT1 : 문화시설
						AG2 : 중개업소
						PO3 : 공공기관
						AT4 : 관광명소
						AD5 : 숙박
						FD6 : 음식점
						CE7 : 카페
						HP8 : 병원
						PM9 : 약국
						
						[예시 매핑]
						- "스타벅스" → CE7
						- "롯데마트" → MT1
						- "세븐일레븐" → CS2
						- "서울역" → SW8
						- "명지병원" → HP8
						- "부동산" → AG2
						- "정보 없음" → null
						
						정답은 오직 코드 하나만 포함된 JSON 형태로 출력해야 하며, 설명, 주석, 마크다운은 절대 포함하지 마라."""),
				new UserMessage(location)
		));


		String origin = (String) request.context().get("origin");
		String destination = (String) request.context().get("destination");


		if (intent.equals("경로")) {

			Map<String, Double> originCoord = kakaoPlaceSearchService.search(origin);
			Map<String, Double> destCoord = kakaoPlaceSearchService.search(destination);

			if (originCoord == null || destCoord == null) {
				log.warn("[LocationResolverAdvisor] 출발역 또는 도착역 위경도 검색 실패 - oring: {}, dest: {}", originCoord, destCoord);
				return chain.nextCall(request);
			}

			log.info("[LocationResolverAdvisor] 출발역 '{}' -> lat={}, lon={}", origin, originCoord.get("lat"), originCoord.get("lon"));
			log.info("[LocationResolverAdvisor] 도착역 '{}' -> lat={}, lon={}", destination, destCoord.get("lat"), destCoord.get("lon"));

			// mutate 해서 각각 위경도 context에 추가
			ChatClientRequest modified = request.mutate()
					.context("originLat",originCoord.get("lat"))
					.context("originLon",originCoord.get("lon"))
					.context("destLat",destCoord.get("lat"))
					.context("destLon",destCoord.get("lon"))
					.build();

			return chain.nextCall(modified);
		}

		if (location == null || location.isBlank()) {
			log.warn("[LocationResolverAdvisor] location 이 누락되어 해당 어드바이저는 스킵합니다.");
			return chain.nextCall(request);
		}

		// intent가 경로가 아닐 경우 기본 location 처리
		String json = chatModel.call(prompt).getResult().getOutput().getText();
		if (!json.trim().startsWith("{")) {
			log.warn("LLM 응답이 JSON이 아님: {}", json);
			return chain.nextCall(request);
		}
		try {
			Map<String, String> extracted = objectMapper.readValue(json, new TypeReference<>() {});
			String category_code = extracted.get(CATEGORY_CODE);
			log.info("category_code : {}", category_code);
			Map<String, Double> coordination = kakaoPlaceSearchService.searchLocation(location, category_code);
			if (coordination == null) {
				log.warn("[LocationResolverAdvisor] kakaoPlaceSearchService.search 결과가 null 입니다. 해당 어드바이저는 스킵합니다. location: {}", location);
				return chain.nextCall(request);
			}

			log.info("[LocationResolverAdvisor] location '{}' → lat={}, lon={}", location, coordination.get("lat"), coordination.get("lon"));

			//  여기만 mutate()로 변경
			ChatClientRequest modified = request.mutate()
					.context("lat", coordination.get("lat"))
					.context("lon", coordination.get("lon"))
					.build();

			return chain.nextCall(modified);

		}catch (Exception e) {
			log.warn("JSON 파싱 실패: {} LLM 응답: {}", e.getMessage(), json, e);
			return chain.nextCall(request);
		}
	}


	@Override
	public String getName() {
		return "LocationResolverAdvisor";
	}

	@Override
	public int getOrder() {
		return 2;
	}
}
