package com.groom.marky.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.marky.domain.ChatLog;
import com.groom.marky.domain.Conversation;
import com.groom.marky.domain.response.ChatLogResponse;
import com.groom.marky.domain.response.ChatResponse;
import com.groom.marky.repository.ChatLogRepository;
import com.groom.marky.repository.ConversationRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class ChatLogService {

	private final ChatLogRepository chatLogRepository;
	private final ConversationRepository conversationRepository;
	private final ObjectMapper objectMapper;

	@Autowired
	public ChatLogService(ChatLogRepository chatLogRepository, ConversationRepository conversationRepository,
		ObjectMapper objectMapper) {
		this.chatLogRepository = chatLogRepository;
		this.conversationRepository = conversationRepository;
		this.objectMapper = objectMapper;
	}

	public List<ChatLogResponse> findByConversationId(String conversationId) {

		Conversation conversation = conversationRepository.findConversationByConversationId(conversationId).orElseThrow(
			() -> new EntityNotFoundException("conversation id에 해당하는 conversaion 엔티티가 존재하지 않습니다.")
		);

		List<ChatLog> chatLogs = chatLogRepository.findChatLogsByConversationOrderByCreatedAtAsc(conversation);

		return chatLogs.stream()
			.map(chatLog -> {
				ChatResponse chatResponse;
				try {
					chatResponse = objectMapper.readValue(chatLog.getAnswer(), ChatResponse.class);

				} catch (Exception e) {
					throw new RuntimeException("Answer JSON 파싱 실패", e);
				}

				return ChatLogResponse.builder()
					.question(chatLog.getQuestion())
					.answer(chatResponse)
					.build();
			})
			.toList();
		}
	}
