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
		- intent: 사용자의 목적 (예: \"카페\", \"경로\" 등)
		- location: 사용자가 원하는 지역/장소명 (예: \"홍대입구\")
		- mood: 사용자 요구의 분위기나 조건 (예: \"조용한\", \"데이트용\")
		- activity_detail: 활동 카테고리 (예: \"스크린야구\")
		- origin, destination: 경로의 출발지와 도착지
		- lat, lon: location의 위도/경도
		- originLat, originLon, destLat, destLon: 경로 위경도 정보
		
		---
		
		사용할 수 있는 도구들:
		1. searchParkingLots(lat, lon)
		2. similaritySearch(mood, ids)
		3. searchActivity(lat, lon, activity_detail)
		4. searchRestaurant(location, mood)
		5. searchCafe(lat, lon, mood)
		6. getSubwayStationList(originLat, originLon, destLat, destLon)
		
		---
		
		️**도구 호출 규칙**
		- context에 필요한 정보가 모두 있는 경우 도구를 호출해야 함
		- 툴 호출 없이 답하지 말고, 결과를 바탕으로 사용자 응답 생성
		- similaritySearch의 인자 중 ids는 List타입으로, searchParkingLots, searchActivity, restaurantSearch, searchCafe의 반환값이 손실없이 그대로 전달되어야 한다.
		
		---
		
		**최종 응답 출력 규칙**
		- 아래 JSON 형태로만 출력해야 함 (절대 설명하지 마)
		```json ``` 으로 감싸지 마.
		- ChatResponse 형식:
		{
		  \"message\": \"사용자에게 보여줄 요약 메시지\",
		  \"places\": [
		    {
		      \"name\": \"...\",
		      \"address\": \"...\",
		      \"latitude\": 0.0,
		      \"longitude\": 0.0,
		      \"rating\": 0.0,
		      \"reviewCount\": 0,
		      \"reviewSummary\": \"...\"
		    }
		  ]
		}
	""";

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		log.info("[MultiPurposeActionAdvisor] 진입");

		Map<String, Object> context = request.context();
		String intent = (String) context.get("intent");
		StringBuilder contextSummary = new StringBuilder("현재 사용자의 context 정보:\n");
		context.forEach((k, v) -> contextSummary.append("- ").append(k).append(": ").append(v).append("\n"));

		String systemPrompt;
		if (intent == null || intent.isBlank()) {
			systemPrompt = """
			사용자의 질문을 이해한 뒤, 어떤 의도로 말했는지 되묻는 역할을 해줘.
			예시: 성동구에서 뭐하고 놀까? → 성동구에서 놀거리와 맛집을 찾고 계신 걸까요?
			""";
		} else if ("액티비티".equals(intent) && context.get("activity_detail") == null) {
			systemPrompt = """
			사용자는 놀거리를 찾고 있어요. 아래 중 하나를 골라서 추천해줄 수 있도록 유도해줘.
			예: 클라이밍, 보드게임카페, 찜질방 등
			""";
		} else {
			systemPrompt = toolHint + "\n\n" + contextSummary;
		}

		Prompt merged = request.prompt().augmentSystemMessage(systemPrompt);
		ChatClientRequest updatedRequest = request.mutate().prompt(merged).build();
		ChatClientResponse response = chain.nextCall(updatedRequest);

		Usage usage = response.chatResponse().getMetadata().getUsage();
		AssistantMessage output = response.chatResponse().getResult().getOutput();
		String userMessageText = (String) context.get("userRawInput");
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
