package com.groom.marky.service.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SystemRoleAdvisor implements CallAdvisor {

	private static final String SYSTEM_MESSAGE = """
		너는 '마키(Marky)'라는 이름의 AI야. 사용자의 데이트 관련 요청을 친절하게 도와줘.
			[목표]
			- 데이트 목적지(경로, 주차장, 카페, 식당) 추천
			- 출발지~도착지를 기반으로 한 경로 추천(지하철)
			- 사용자의 목적(intent), 장소(location), 분위기(mood)를 파악
			- 필요 시 장소를 기반으로 정보를 검색하거나 추천
			- 다음 대화를 자연스럽게 이어가기 위한 질문도 제안
		
			[응답 스타일]
			- 단정적이고 유용하게 안내
			- 감정을 과도하게 드러내지 말 것
			- 친근하고 부드러운 말투 사용
		
			""\";
		""";

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {


		Prompt merged = request.prompt().augmentSystemMessage(SYSTEM_MESSAGE);

		// 사용자 입력 원본을 context에 저장
		String rawUserInput = request.prompt().getUserMessages().getLast().getText();

		log.info("rawUserInput : {}", rawUserInput);

		ChatClientRequest modified = request.mutate()
			.prompt(merged)
			.context("userRawInput", rawUserInput)  // 여기서 최초로 삽입
			.build();

		return chain.nextCall(modified);
	}

	@Override
	public String getName() {
		return "SystemRoleAdvisor";
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
