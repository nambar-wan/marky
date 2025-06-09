package com.groom.marky.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.marky.domain.response.ChatLogResponse;
import com.groom.marky.domain.response.ConversationResponse;
import com.groom.marky.service.ChatLogService;
import com.groom.marky.service.ConversationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/conversations")
public class ConversationController {

	// /
	private final ConversationService conversationService;
	private final ChatLogService chatLogService;

	@Autowired
	public ConversationController(ConversationService conversationService, ChatLogService chatLogService) {
		this.conversationService = conversationService;
		this.chatLogService = chatLogService;
	}

	@GetMapping("/me")
	public ResponseEntity<?> getMyConversations(@AuthenticationPrincipal UserDetails userDetails) {

		String userEmail = userDetails.getUsername();
		List<ConversationResponse> conversations = conversationService.findByUserEmail(userEmail);

		return ResponseEntity.ok(conversations);
	}


	@GetMapping("/{conversationId}/chats")
	public ResponseEntity<?> getChatsByConversationId(@PathVariable String conversationId) {

		log.info("conversationId : {}", conversationId);

		List<ChatLogResponse> chatLogs = chatLogService.findByConversationId(conversationId);

		return ResponseEntity.ok(chatLogs);
	}
}
