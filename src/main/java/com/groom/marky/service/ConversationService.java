package com.groom.marky.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.marky.domain.Conversation;
import com.groom.marky.domain.User;
import com.groom.marky.domain.request.CreateConversationRequest;
import com.groom.marky.domain.response.ConversationResponse;
import com.groom.marky.repository.ConversationRepository;
import com.groom.marky.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class ConversationService {

	private final ConversationRepository conversationRepository;
	private final UserRepository userRepository;

	@Autowired
	public ConversationService(ConversationRepository conversationRepository, UserRepository userRepository) {
		this.conversationRepository = conversationRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public ConversationResponse create(CreateConversationRequest request) {

		String userEmail = request.getUserEmail();
		User user = userRepository.findUserByUserEmail(userEmail).orElseThrow(
			() -> new EntityNotFoundException("해당되는 유저정보가 존재하지 않습니다.")
		);

		Conversation conversation = Conversation.builder()
			.conversationId(request.getConversationId())
			.user(user)
			.build();

		Conversation savedConversation = conversationRepository.save(conversation);

		return ConversationResponse.builder()
			.conversationId(savedConversation.getConversationId())
			.build();

	}

	@Transactional
	public void setTitle(ConversationResponse conversationResponse, String title) {
		String conversationId = conversationResponse.getConversationId();

		Conversation conversation = conversationRepository.findConversationByConversationId(conversationId).orElseThrow(
			() -> new EntityNotFoundException("해당되는 conversation 이 존재하지 않습니다.")
		);

		conversation.setTitle(title);

	}
}
