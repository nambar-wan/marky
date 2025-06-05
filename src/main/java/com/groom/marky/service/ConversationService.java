package com.groom.marky.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.marky.domain.response.ConversationResponse;
import com.groom.marky.domain.Conversation;
import com.groom.marky.domain.User;
import com.groom.marky.domain.request.CreateConversationRequest;
import com.groom.marky.domain.response.CreateConversationResponse;
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
	public CreateConversationResponse create(CreateConversationRequest request) {

		String userEmail = request.getUserEmail();
		User user = userRepository.findUserByUserEmail(userEmail).orElseThrow(
			() -> new EntityNotFoundException("해당되는 유저정보가 존재하지 않습니다.")
		);

		Conversation conversation = Conversation.builder()
			.conversationId(request.getConversationId())
			.user(user)
			.build();

		Conversation savedConversation = conversationRepository.save(conversation);

		return CreateConversationResponse.builder()
			.conversationId(savedConversation.getConversationId())
			.build();

	}

	@Transactional
	public void setTitle(CreateConversationResponse conversationResponse, String title) {
		String conversationId = conversationResponse.getConversationId();

		Conversation conversation = conversationRepository.findConversationByConversationId(conversationId).orElseThrow(
			() -> new EntityNotFoundException("해당되는 conversation 이 존재하지 않습니다.")
		);

		conversation.setTitle(title);

	}

	public List<ConversationResponse> findByUserEmail(String userEmail) {

		User user = userRepository.findUserByUserEmail(userEmail).orElseThrow(
			() -> new EntityNotFoundException("해당되는 유저 정보가 존재하지 않습니다.")
		);

		List<Conversation> conversations = conversationRepository.findConversationsByUserOrderByCreatedAtDesc(user);

		return conversations.stream()
			.map(conversation ->
					ConversationResponse.builder()
						.title(conversation.getTitle())
						.conversationId(conversation.getConversationId())
						.createdAt(conversation.getCreatedAt()) // Optional: UI용
						.build()
			).toList();
	}
}
