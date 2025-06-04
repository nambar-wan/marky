package com.groom.marky.controller;

import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groom.marky.domain.request.CreateChatRequest;
import com.groom.marky.domain.request.CreateConversationRequest;
import com.groom.marky.domain.response.ConversationResponse;
import com.groom.marky.service.ChatClientFactory;
import com.groom.marky.service.ConversationService;
import com.groom.marky.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class ChatController {

	private final ChatClient chatClient;
	private final ChatClientFactory chatClientFactory;
	private final ConversationService conversationService;
	private final ChatModel chatModel;

	@Autowired
	public ChatController(ChatClient chatClient, ChatClientFactory chatClientFactory,
		ConversationService conversationService, ChatModel chatModel) {
		this.chatClient = chatClient;
		this.chatClientFactory = chatClientFactory;
		this.conversationService = conversationService;
		this.chatModel = chatModel;
	}

	@GetMapping("/ai")
	public String chat(@RequestParam String message) {
		ChatClient.CallResponseSpec response = chatClient.prompt()
			.user(message)
			.call();

		return response.content();

	}

	/**
	 * 아래는 테스트입니다.
	 */
	@PostMapping("/chat/ai")
	public String chat(@RequestBody CreateChatRequest request, @AuthenticationPrincipal UserDetails userDetails) {

		String conversationId = request.getConversationId();
		String message = request.getMessage();
		String userEmail = userDetails.getUsername();

		if (conversationId == null) {
			// 첫 대화. 새로 생성
			// 프롬프트 간단하게 한번 돌려서 메세지 제목 생성
			CreateConversationRequest conversationRequest = CreateConversationRequest.builder()
				.conversationId(UUID.randomUUID().toString())
				.userEmail(userEmail)
				.build();

			ConversationResponse conversationResponse = conversationService.create(conversationRequest);

			ChatClient chatClientWithoutAdvisor = ChatClient.builder(chatModel).build();

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

		return client.prompt()
			.user(message)
			.advisors()
			.call()
			.content();
	}
}
