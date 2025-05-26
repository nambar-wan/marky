package com.groom.marky.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

	private final ChatClient chatClient;

	@Autowired
	public ChatController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@GetMapping("/ai")
	public String chat(@RequestParam String message) {
		return chatClient.prompt()
			.user(message)
			.call()
			.content();
	}

}
