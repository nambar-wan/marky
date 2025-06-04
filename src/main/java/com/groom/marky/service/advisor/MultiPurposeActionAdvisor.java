package com.groom.marky.service.advisor;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Description;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("사용자의 intent, location, mood context를 기반으로 searchParkingLots와 searchActivity와 similaritySearch 툴 호출을 구성하고 실행을 유도하는 어드바이저")
public class MultiPurposeActionAdvisor implements CallAdvisor {

	private static final String toolHint = """
		너는 사용자의 요청을 이해하고, context 정보를 기반으로 적절한 도구(tool)를 호출한 뒤, 
		그 결과를 사용자에게 자연스럽고 친절하게 요약해주는 역할이야.
		
		너는 다음 context 정보를 입력으로 받게 돼:
		- intent: 사용자의 목적 (예: "카페", "경로" 등)
		- location: 사용자가 원하는 지역/장소명 (예: "홍대입구")
		- mood: 사용자 요구의 분위기나 조건 (예: "조용한", "데이트용")
		- activity_detail: 활동 카테고리 (예: "스크린야구")
		- origin, destination: 경로의 출발지와 도착지
		- lat, lon: location의 위도/경도
		- originLat, originLon, destLat, destLon: 경로 위경도 정보
		
		---
		
		사용할 수 있는 도구들:
		
		1. **searchParkingLots(lat, lon)**  
		   - 사용자의 위치(lat, lon)를 기준으로 반경 2km 이내의 주차장 ID 목록을 조회  
		   - Geo 기반 Redis 검색을 사용
		
		2. **similaritySearch(mood, ids)**  
		   - 주차장 ID나 액티비티 ID와 함께 mood가 있는 경우  
		   - 의미적으로 유사한 장소를 5개 추천 (pgvector 임베딩 활용)
		
		3. **searchActivity(lat, lon, activity_detail)**  
		   - 사용자의 위치와 원하는 액티비티가 명확히 존재할 때  
		   - 반경 2km 이내에서 해당 활동을 할 수 있는 장소 탐색
		
		4. **restaurantSearch(location, mood)**  
		   - 특정 지역(location)과 분위기(mood)가 주어졌을 때  
		   - 적절한 식당을 추천
		
		5. **cafeSearch(lat, lon, mood)**  
		   - 위도/경도와 분위기 기반으로 근처 카페 추천
		
		6. **getSubwayStationList(originLat, originLon, destLat, destLon)**  
		   - 출발지와 도착지 좌표가 모두 있는 경우  
		   - 지하철 경유 역 리스트를 조회 (Tmap API 기반)
		
		---
		
		️ **도구 호출 규칙 (꼭 지켜야 함)**
		
		- context에 필요한 정보가 **모두 있는 경우, 반드시 해당 툴을 호출해야 해.**
		- 가능한 한 **여러 도구를 순차적으로 호출**하여 풍부한 정보를 제공해도 좋아.
		- 툴 호출 없이 사용자 질문에만 답하지 마. 도구를 통해 얻은 결과를 바탕으로 답을 구성해.
		
		---
		
		**사용자 응답 형식 규칙**
		
		- 툴 호출 결과를 바탕으로 장소/경로/주차장 정보를 **자연스럽고 친절한 문장**으로 정리해줘.
		- 가능한 경우 장소 이름, 위치, 특징(예: "조용한 분위기", "리뷰가 좋은")도 함께 전달해.
		- 마크다운(```)이나 JSON은 절대 사용하지 마.
		- 도구 결과가 없거나 빈 목록일 경우, 이유를 설명하고 대안이나 유사 항목을 제안해줘.
		
		---
		
		이제 너의 역할은 단순한 대답이 아니라,
		**도구 호출 → 결과 해석 → 사용자 응답 생성까지 전체 책임을 지는 것**이야.
		반드시 context에 맞는 도구를 호출하고, 그 결과를 바탕으로 정중하고 유용한 응답을 생성해줘.
		""";

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		log.info("[MultiPurposeActionAdvisor] 진입");

		Map<String, Object> context = request.context();
		String intent = (String)context.get("intent");

		context.forEach((k, v) -> log.info("key : {}, value : {}", k, v));

		StringBuilder contextSummary = new StringBuilder("현재 사용자의 context 정보:\n");
		context.forEach((k, v) -> contextSummary.append("- ").append(k).append(": ").append(v).append("\n"));

		String systemPrompt;

		if (intent == null || intent.isBlank()) {
			log.warn("[MultiPurposeActionAdvisor] intent 없음 → 사용자의 질문을 기반으로 의도 추정 요청");

			systemPrompt = """
				사용자의 질문을 이해한 뒤, 사용자가 어떤 의도로 말했는지 되묻는 역할을 해줘.
				
				예시:
				> 사용자: 성동구에서 뭐하고 놀까?
				> 응답: 성동구에서 놀거리와 맛집을 찾고 계신 걸까요?
				
				""";
		} else if ("액티비티".equals(intent) && context.get("activity_detail") == null) {
			log.info("[MultiPurposeActionAdvisor] 액티비티인데 activity_detail 누락됨 → 사용자에게 선택 유도");

			systemPrompt = """
				사용자는 놀거리를 찾고 있어요. 아래 중 하나를 골라서 추천해줄 수 있도록 유도해줘.
				답변은 자연어로, 친절하고 가볍게 물어보듯 말해.
				
				선택지 예시:
				- 클라이밍
				- 스크린야구
				- 스크린골프
				- 보드게임카페
				- 만화카페
				- 방탈출
				- VR체험관
				- PC방
				- 볼링장
				- 당구장
				- 아쿠아리움
				- 찜질방
				- 시장
				
				> 예시 응답:
				성동구 근처에서 어떤 활동을 원하시나요? 클라이밍, 보드게임카페, 찜질방 같은 여러 가지가 있어요!
				""";

		} else {
			systemPrompt = toolHint + "\n\n" + contextSummary;
		}

		Prompt merged = request.prompt()
			.augmentSystemMessage(systemPrompt);

		ChatClientRequest updatedRequest = request.mutate()
			.prompt(merged)
			.build();

		ChatClientResponse response = chain.nextCall(updatedRequest);

		Usage usage = response.chatResponse().getMetadata().getUsage();
		UserMessage userMessage = request.prompt().getUserMessage();
		AssistantMessage output = response.chatResponse().getResult().getOutput();

		String userMessageText = userMessage.getText();
		String outputText = output.getText();
		log.info("userMessage : {}, output : {}", userMessageText, outputText);

		output.getMetadata().put("question", userMessageText);
		output.getMetadata().put("answer", outputText);
		output.getMetadata().put("usage", usage);

		return response;
	}

	@Override
	public String getName() {
		return "MultiPurposeActionAdvisor";
	}

	@Override
	public int getOrder() {
		return 4;
	}
}
