package com.groom.marky.service.advisor;

import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("사용자의 메시지에서 intent, location, mood를 추출하는 어드바이저")
public class UserIntentAdvisor implements CallAdvisor {

	private final ChatModel chatModel;
	private final ObjectMapper objectMapper;

	private static final String INTENT_KEY = "intent";
	private static final String LOCATION_KEY = "location";
	private static final String MOOD_KEY = "mood";
	private static final String ORIGIN = "origin";
	private static final String DESTINATION = "destination";
	private static final String TIME_SLOT = "timeSlot";
	private static final String DAY_TYPE = "dayType";


	@Autowired
	public UserIntentAdvisor(ChatModel chatModel, ObjectMapper objectMapper) {
		this.chatModel = chatModel;
		this.objectMapper = objectMapper;
	}

	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

		log.info("[UserIntentAdvisor] 진입");

		String input = request.prompt().getUserMessages().stream()
			.map(UserMessage::getText)
			.reduce(" ", String::concat);

		// 프롬프트 상세히 쓰기..
		// 지피티한테 프롬프트 짜달라하기
		Prompt prompt = new Prompt(List.of(
			new SystemMessage("""
					다음 사용자 입력에서 intent, location, mood를 JSON 형식으로 정확히 추출해줘.
				
					- intent: 사용자의 목적 (예: 주차장, 카페, 식당, 놀거리, 경로, 지하철 등)
					- location: 장소명 또는 지역명 (예: 연남동, 홍대입구역, 마포구 등)
					- mood: 분위기, 선호 조건, 상황 (예: 조용한, 디저트가 맛있는, 공부하기 좋은, 감성적인 등)
				
					mood에는 다음과 같은 유형이 포함될 수 있어:
					- 음식 취향: 느끼한 음식, 매운 음식, 건강한 음식 등
					- 분위기: 조용한, 분위기 좋은, 감성적인, 트렌디한 등
					- 목적/상황: 공부하기 좋은, 연인과 가기 좋은, 아이와 가기 좋은 등
					- 편의 조건: 주차 가능한, 테이크아웃 가능, 콘센트 있는 등
					
					[경로 요청 처리]
					- 사용자가 "A역에서 B역까지 가는 법", "A~B 지하철 경로", "A역에서 B역까지 지하철 경로 알려줘", "몇시 요일 A역에서 B역까지 지하철 경로 알려줘"등을 말하면
					- intent : "경로"
					- mood는 비워두고 "" 처리해줘.
					- location는 비워두고 "" 처리해줘
					- intent가 "경로"라면 출발지를 유추해서 origin 키로,  도착지를 유추해서 destination 키로 저장해줘.
					- timeSlot: "시간" 사용자가 입력한게 없다면 ""
					- dayType: "요일" 사용자가 입력한게 없다면 ""
					 
				
					[위치 정규화 안내]
					- 사용자가 "근처", "주변", "가까운", "인근" 등의 표현을 사용하면,
					  실제 존재하는 지명으로 location 값을 보정해서 추출해줘.
					- 예를 들어 "홍대입구역 근처"는 location: "홍대입구역" 으로 변환.
					- 잘 알려진 지하철역, 동/구 단위 행정동 등을 우선적으로 활용해.
					- 존재하지 않는 장소명일 경우 가장 유사한 실제 장소로 추정해줘.
				
					출력은 반드시 아래 JSON 형식을 따라줘. 키는 모두 포함되어야 하며, 값이 없을 경우 "null"로 작성해.
				
					출력 예시:
					{ "intent": "식당", "location": "합정역", "mood": "조용하고 분위기 좋은" }
					{ "intent": "경로", "location": "","origin": "강남역", "destination": "역삼역", "mood": "", "timeSlot" : "",
					  "dayType": ""}
					
				
					반드시 JSON만 출력하고, 설명은 포함하지 마.
					너는 툴 콜링을 사용하면 안돼.
				"""),
			new UserMessage(input)
		));

		String json = chatModel.call(prompt).getResult().getOutput().getText();


		if (!json.trim().startsWith("{")) {
			log.warn("LLM 응답이 JSON이 아님: {}", json);
			return chain.nextCall(request);
		}



		try {
			Map<String, String> extracted = objectMapper.readValue(json, new TypeReference<>() {});
			String intent = extracted.get(INTENT_KEY);
			String location = extracted.getOrDefault(LOCATION_KEY,"");
			String mood = extracted.getOrDefault(MOOD_KEY,"");
			String origin = extracted.get(ORIGIN);
			String destination = extracted.get(DESTINATION);
			String timeSlot = extracted.get(TIME_SLOT);
			String dayType = extracted.get(DAY_TYPE);

			log.info("[UserIntentAdvisor] 의도: {}, 장소: {}, 분위기: {}, 출발지 : {}, 목적지 : {}" , intent, location, mood, origin, destination);


			ChatClientRequest modified = request.mutate()
				.context(INTENT_KEY, intent)
				.context(LOCATION_KEY, location)
				.context(MOOD_KEY, mood)
				.context(ORIGIN,origin)
				.context(DESTINATION,destination)
				.context(TIME_SLOT,timeSlot)
				.context(DAY_TYPE,dayType)
				.build();

			return chain.nextCall(modified);

		} catch (Exception e) {
			log.warn("JSON 파싱 실패: {} LLM 응답: {}", e.getMessage(), json, e);
			return chain.nextCall(request);
		}
	}

	@Override
	public String getName() {
		return "UserIntentAdvisor";
	}

	@Override
	public int getOrder() {
		return 1;
	}
}
