package com.groom.marky.controller;


import java.util.ArrayList;
import java.util.List;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.marky.domain.request.CreateChatRequest;
import com.groom.marky.domain.request.CreateConversationRequest;
import com.groom.marky.domain.response.ChatResponse;
import com.groom.marky.domain.response.CreateConversationResponse;
import com.groom.marky.service.ChatClientFactory;
import com.groom.marky.service.ConversationService;

import lombok.extern.slf4j.Slf4j;


@RestController
public class ChatController {

	private final ChatClientFactory chatClientFactory;
	private final ConversationService conversationService;
	private final ChatModel chatModel;
	private final ObjectMapper objectMapper;

	@Autowired
	public ChatController(ChatClientFactory chatClientFactory,
		ConversationService conversationService, ChatModel chatModel, ObjectMapper objectMapper) {
		this.chatClientFactory = chatClientFactory;
		this.conversationService = conversationService;
		this.chatModel = chatModel;
		this.objectMapper = objectMapper;
	}


	@PostMapping("/chat/ai")
	public ResponseEntity<?> chat(@RequestBody CreateChatRequest request, @AuthenticationPrincipal UserDetails userDetails) {

		String conversationId = request.getCid();
		String message = request.getMessage();
		String userEmail = userDetails.getUsername();

		log.info("message : {} ", message);

		ChatClient chatClientWithoutAdvisor;

		if (conversationId == null) {
			// 첫 대화. 새로 생성
			// 프롬프트 간단하게 한번 돌려서 메세지 제목 생성
			CreateConversationRequest conversationRequest = CreateConversationRequest.builder()
				.conversationId(UUID.randomUUID().toString())
				.userEmail(userEmail)
				.build();

			CreateConversationResponse conversationResponse = conversationService.create(conversationRequest);

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
				   ""\"
			 너는 사용자의 복합 질문을 의미 단위로 "분리"하는 역할만 수행해.
			\s
			 - 문장의 의미를 절대로 바꾸지 마.
			 - 표현 방식이나 말투도 바꾸지 마.
			 - 말 그대로, 원문을 그대로 유지하면서 "질문 단위"로 나누기만 해.
			 - 각 문장은 줄바꿈(\\\\n)으로 구분해.
			 - 리스트나 번호는 붙이지 마.
			 - 띄어쓰기만 보정하는 건 괜찮아.
				
			 예시:
			 입력: 홍대입구역 근처는??
			 출력:
			 홍대입구역 근처는??
			\s
			 입력: 강남역에서 파스타 먹고 영화 보고 싶어
			 출력:
			 강남역에서 파스타 먹고 싶어
			 강남역에서 영화 보고 싶어
				
			 입력: 홍대에서 분위기 좋은 카페 갔다가, 저녁엔 신촌에서 조용한 식당 가고 싶어
			 출력:
			 홍대에서 분위기 좋은 카페 가고 싶어
			 저녁엔 신촌에서 조용한 식당 가고 싶어
			 ""\"
				""")
			.user(message)
			.advisors()
			.call()
			.content();

		String[] questionArray = userQuestions.split("\\n");

		List<ChatResponse> chatResponses = new ArrayList<>();

		for (String q : questionArray) {
			log.info("transformedMessage : {} ", q);

			String rawOutput = client.prompt()
				.user(q)
				.advisors()
				.call()
				.content();

			try {
				ChatResponse chatResponse = objectMapper.readValue(rawOutput, ChatResponse.class);
				chatResponses.add(chatResponse);
			} catch (Exception e) {
				log.warn("[ChatController] JSON 파싱 실패: {}", e.getMessage());

				// fallback: LLM 응답을 그냥 텍스트로 넣기
				ChatResponse fallback = new ChatResponse();
				fallback.setMessage(rawOutput);
				chatResponses.add(fallback);
			}
		}

		// 여러 질문 → 여러 응답 (JSON 파싱된 ChatResponse 리스트)
		return ResponseEntity.ok(chatResponses);

	}
}
