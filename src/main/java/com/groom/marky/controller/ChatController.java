package com.groom.marky.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ChatController {

	private final ChatClient chatClient;

	@Autowired
	public ChatController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@GetMapping("/ai")
	public String chat(@RequestParam String message) {

		long start = System.currentTimeMillis();

		String response =  chatClient.prompt()
				.user(message)
				.call()
				.content();

		long end = System.currentTimeMillis();

		log.info("요청 처리 시간: {} ms", end-start);

		return response;
	}

}
