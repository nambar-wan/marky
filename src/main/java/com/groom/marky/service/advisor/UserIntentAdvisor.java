package com.groom.marky.service.advisor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.*;
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

	private static final Map<String, String> DEFAULT_MOOD_BY_INTENT = Map.ofEntries(
		Map.entry("카페", "리뷰가 있고, 사용자 평가가 좋은"),
		Map.entry("식당", "리뷰가 있고, 사용자 평가가 좋은"),
		Map.entry("액티비티", "리뷰가 있고, 사용자 평가가 좋은"),
		Map.entry("주차장", "리뷰가 있고, 사용자 평가가 좋은")
	);

	@Autowired
	public UserIntentAdvisor(ChatModel chatModel, ObjectMapper objectMapper) {
		this.chatModel = chatModel;
		this.objectMapper = objectMapper;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		log.info("[UserIntentAdvisor] 진입");

		List<UserMessage> allUserMessages = request.prompt().getUserMessages();
		log.info("userMessages 수 : {}", allUserMessages.size());

		Map<String, Object> extracted = tryExtractContext(allUserMessages);

		if (extracted == null || extracted.get(INTENT_KEY) == null || extracted.get(INTENT_KEY).toString().isBlank()) {
			log.warn("[UserIntentAdvisor] 추출 실패 → 원본 요청 진행");
			return chain.nextCall(request);
		}

		String intent = (String)extracted.getOrDefault(INTENT_KEY, "");
		String location = (String)extracted.getOrDefault(LOCATION_KEY, "");
		String mood = (String)extracted.getOrDefault(MOOD_KEY, "");
		String origin = (String)extracted.getOrDefault(ORIGIN, "");
		String destination = (String)extracted.getOrDefault(DESTINATION, "");
		String timeSlot = (String)extracted.getOrDefault(TIME_SLOT, "");
		String dayType = (String)extracted.getOrDefault(DAY_TYPE, "");

		if ((mood == null || mood.isBlank()) && !intent.isBlank()) {
			mood = DEFAULT_MOOD_BY_INTENT.getOrDefault(intent, "");
			log.info("[UserIntentAdvisor] mood 기본값 적용: {}", mood);
		}

		if ("경로".equals(intent)) {
			if (timeSlot.isBlank()) {
				LocalDateTime now = LocalDateTime.now();
				timeSlot = now.format(DateTimeFormatter.ofPattern("a h시")).replace("AM", "오전").replace("PM", "오후");
				log.info("[UserIntentAdvisor] timeSlot 기본값 적용: {}", timeSlot);
			}

			if (dayType.isBlank()) {
				DayOfWeek dow = LocalDateTime.now().getDayOfWeek();
				dayType = switch (dow) {
					case SATURDAY, SUNDAY -> "주말";
					default -> dow.getDisplayName(java.time.format.TextStyle.FULL, Locale.KOREAN);
				};
				log.info("[UserIntentAdvisor] dayType 기본값 적용: {}", dayType);
			}
		}

		return chain.nextCall(request.mutate()
			.context(INTENT_KEY, intent)
			.context(LOCATION_KEY, location)
			.context(MOOD_KEY, mood)
			.context(ORIGIN, origin)
			.context(DESTINATION, destination)
			.context(TIME_SLOT, timeSlot)
			.context(DAY_TYPE, dayType)
			.build());
	}

	private Map<String, Object> tryExtractContext(List<UserMessage> userMessages) {
		List<Message> messages = new ArrayList<>();

		// 시스템 메시지 추가
		messages.add(new SystemMessage("""
			다음 사용자 입력에서 intent, location, mood를 JSON 형식으로 정확히 추출해줘.

			너는 사용자의 문장을 이해하고, 아래 5가지 중 하나로 intent 값을 지정해줘.

			[intent 값 목록]
			- "카페"
			- "식당"
			- "경로"
			- "주차장"
			- "액티비티"

			[분류 규칙]
			- 문장에 "카페", "커피", "디저트"가 포함되면 intent = "카페"
			- "식당", "맛집", "음식", "밥집"이 포함되면 intent = "식당"
			- "어떻게 가", "가는 방법", "길 안내", "도착", "경로"가 포함되면 intent = "경로"
			- "주차", "주차장"이 포함되면 intent = "주차장"
			- 다음 키워드 중 하나라도 포함되면 intent = "액티비티":
			  - **13가지 활동 키워드**: "클라이밍", "스크린야구", "스크린골프", "보드게임카페", "만화카페",
			    "방탈출", "VR체험관", "PC방", "볼링장", "당구장", "아쿠아리움", "찜질방", "시장"
			  - **일반 활동 표현**: "놀거리", "할거리", "할거" 등의 표현

			[추출 대상 필드]
			- intent: 위 기준 중 해당되는 활동 유형
			- location: 장소명 또는 지역명 (예: 강남역, 연남동, 마포구 등)
			- mood: 분위기, 선호 조건, 상황 (예: 조용한, 트렌디한, 디저트 맛있는, 공부하기 좋은 등)

			⚠️ **주의 사항**:
			- location 값에는 "근처", "그 근처", "이 근처", "주변"과 같은 모호한 단어를 그대로 사용하지 마.
			- 대신, 반드시 최근 대화 내용을 참고하여 구체적인 장소명을 추론해서 location에 넣어.
			- 현재 메시지에서 location, intent, mood 가 명확하지 않다면 최근 대화 내용을 기반으로 적극적으로 유추해.
			- 최근 대화 메시지는 여러 개 존재하니 충분히 참고해서 가장 신뢰도 높은 정보를 뽑아줘.

			[경로(intent=경로) 요청일 경우 추가로 추출할 필드]
			- intent: 반드시 "경로"
			- location과 mood는 ""로 비워줘
			- origin: 출발지
			- destination: 도착지
			- timeSlot: 시간 관련 표현이 있다면 추출, 없으면 ""
			- dayType: 요일 표현이 있다면 추출, 없으면 ""

			[출력 형식]
			- 모든 키를 포함하고, 값이 없으면 ""로 출력해
			- 반드시 아래 형식의 JSON만 출력해. 절대 설명하지 마. 툴 콜링도 하지 마.

			{
			  "intent": "...",
			  "location": "...",
			  "mood": "...",
			  "origin": "...",
			  "destination": "...",
			  "timeSlot": "...",
			  "dayType": "..."
			}
			"""));

		// 최근 사용자 메시지 최대 10개만 포함
		int fromIndex = Math.max(0, userMessages.size() - 10);
		messages.addAll(userMessages.subList(fromIndex, userMessages.size()));

		try {
			Prompt prompt = new Prompt(messages);
			String raw = chatModel.call(prompt).getResult().getOutput().getText();
			String json = raw.replaceAll("(?s)^```json\\s*", "").replaceAll("(?s)```$", "").trim();

			if (!json.startsWith("{"))
				return null;

			return objectMapper.readValue(json, new TypeReference<>() {
			});
		} catch (Exception e) {
			log.warn("[UserIntentAdvisor] JSON 파싱 실패: {}", e.getMessage());
			return null;
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
