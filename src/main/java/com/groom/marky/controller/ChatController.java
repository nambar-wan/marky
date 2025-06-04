package com.groom.marky.controller;

import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.groom.marky.domain.request.CreateChatRequest;
import com.groom.marky.domain.request.CreateConversationRequest;
import com.groom.marky.domain.response.ConversationResponse;
import com.groom.marky.service.ChatClientFactory;
import com.groom.marky.service.ConversationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class ChatController {

	private final ChatClientFactory chatClientFactory;
	private final ConversationService conversationService;
	private final ChatModel chatModel;

	@Autowired
	public ChatController(ChatClientFactory chatClientFactory,
		ConversationService conversationService, ChatModel chatModel) {
		this.chatClientFactory = chatClientFactory;
		this.conversationService = conversationService;
		this.chatModel = chatModel;
	}

	/**
	 * 아래는 테스트입니다.
	 */
	@PostMapping("/chat/ai")
	public ResponseEntity<?> chat(@RequestBody CreateChatRequest request, @AuthenticationPrincipal UserDetails userDetails) {

		String conversationId = request.getCid();
		String message = request.getMessage();
		String userEmail = userDetails.getUsername();

		ChatClient chatClientWithoutAdvisor;

		if (conversationId == null) {
			// 첫 대화. 새로 생성
			// 프롬프트 간단하게 한번 돌려서 메세지 제목 생성
			CreateConversationRequest conversationRequest = CreateConversationRequest.builder()
				.conversationId(UUID.randomUUID().toString())
				.userEmail(userEmail)
				.build();

			ConversationResponse conversationResponse = conversationService.create(conversationRequest);

			chatClientWithoutAdvisor = ChatClient.builder(chatModel).build();

			// 입력 메시지를 프롬프트로 사용하여 제목 생성
			String title = chatClientWithoutAdvisor.prompt()
				.system("다음 사용자의 입력을 바탕으로, 이 대화의 제목을 한 문장으로 생성해줘.")
				.user(message)
				.advisors()
				.call()
				.content();

			// 제목 넣기
			conversationService.setTitle(conversationResponse, title);
			conversationId = conversationResponse.getConversationId();
		}

		ChatClient client = chatClientFactory.create(conversationId);

		// 사용자 입력에 다중 목적이 포함되어 있다면, 이를 2개 이상의 분리된 질문으로 나눔
		chatClientWithoutAdvisor = ChatClient.builder(chatModel).build();

		String userQuestions = chatClientWithoutAdvisor.prompt()
			.system("""
				    너는 사용자의 복합 질문을 의미 단위로 나누는 역할이야.
				    사용자의 문장 안에 두 개 이상의 요청이 있으면, 각각 별도의 문장으로 분리해줘.
				
				    - 불필요한 말은 제거하지 말고, 원문의 의미를 최대한 유지해.
				    - 각 문장은 단독으로 이해될 수 있도록 작성해.
				    - 각 문장은 줄바꿈(\\n)으로 구분해줘.
				    - 숫자나 리스트 형태로 출력하지 마. 그냥 문장만 나열해.
				
				    예시:
				    입력: 강남역에서 파스타 먹고 영화 보고 싶어
				    출력:
				    강남역에서 파스타 먹고 싶어
				    강남역에서 영화 보고 싶어
				
				    입력: 홍대에서 분위기 좋은 카페 갔다가, 저녁엔 신촌에서 조용한 식당 가고 싶어
				    출력:
				    홍대에서 분위기 좋은 카페 가고 싶어
				    저녁엔 신촌에서 조용한 식당 가고 싶어
				""")
			.user(message)
			.advisors()
			.call()
			.content();

		String[] questionArray = userQuestions.split("\\n");

		StringBuilder combinedResponse = new StringBuilder();

		for (String q : questionArray) {
			String response = client.prompt()
				.user(q)
				.advisors()
				.call()
				.content();

			combinedResponse.append(response).append("\n\n"); // 응답 간 개행 추가
		}

		String finalResponse = combinedResponse.toString().trim();
		return ResponseEntity.ok(finalResponse);

	}
}
