package com.groom.marky.repository;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.groom.marky.domain.ChatLog;
import com.groom.marky.domain.Conversation;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@Transactional(readOnly = true)
public class CustomChatMemoryRepository implements ChatMemoryRepository {

	private final ChatLogRepository chatLogRepository;
	private final ConversationRepository conversationRepository;

	@Autowired
	public CustomChatMemoryRepository(ChatLogRepository chatLogRepository,
		ConversationRepository conversationRepository) {
		this.chatLogRepository = chatLogRepository;
		this.conversationRepository = conversationRepository;
	}

	/**
	 * 역할: 저장된 모든 conversationId 리스트를 반환
	 * @return
	 */
	@Override
	public List<String> findConversationIds() {
		return conversationRepository.findAllConversationIds();
	}

	/**
	 * 역할: 지정된 대화(conversationId)에 해당하는 전체 메시지를 시간순으로 가져오기
	 * Spring AI 내부 어드바이저가 ChatMemory 를 사용할 때 자동 호출
	 * 인자로 넘어오는 conversationId를 받기 위해, 어드바이저에서 conversation 을 미리 만들어야함.
	 * @param conversationId
	 * @return
	 */
	@Override
	public List<Message> findByConversationId(String conversationId) {
		log.info("[CustomChatMemoryRepository] findByConversationId 진입. conversationId : {} ", conversationId);

		// 1. conversationId로 ChatLog 리스트 조회
		try {
			Conversation conversation = conversationRepository.findConversationByConversationId(conversationId)
				.orElseThrow(
					() -> new EntityNotFoundException("해당되는 아이디의 conversation 엔티티가 존재하지 않습니다.")
				);

			List<ChatLog> logs = chatLogRepository.findTop10ByConversationOrderByCreatedAtAsc(conversation);

			// 2. ChatLog 를 Spring AI의 Message 객체로 변환

			// mapMulti()는 입력 1개 → 출력 0개 이상을 만들 수 있습니다.
			return logs.stream()
				.<Message>mapMulti((log, sink) -> {
					sink.accept(new UserMessage(log.getQuestion()));
					sink.accept(new AssistantMessage(log.getAnswer()));
				})
				.toList();
		} catch (EntityNotFoundException e) {
			log.warn("ChatMemory: Conversation 미존재: {}", conversationId);
			return List.of();
		}
	}

	/**
	 * 역할: 특정 대화 ID에 대한 메시지들을 저장
	 * LLM 응답이 완료된 후, 즉 질문 → 응답까지 끝난 다음에 호출됨.
	 * @param conversationId
	 * @param messages
	 */
	@Transactional
	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		log.info("[CustomChatMemoryRepository] saveAll 진입. conversationId : {}", conversationId);

		Conversation conversation = conversationRepository.findConversationByConversationId(conversationId)
			.orElse(null);

		if (conversation == null) {
			log.warn("저장 스킵: conversationId {} 에 해당하는 Conversation이 없음", conversationId);
			return;
		}

		log.info("message size: {}", messages.size());

		for (Message message : messages) {
			if (message instanceof AssistantMessage) {
				Map<String, Object> metadata = message.getMetadata();

				if (metadata == null || metadata.isEmpty()) {
				//	log.warn("저장 스킵: metadata가 비어 있음");
					continue;
				}

				Object questionObj = metadata.get("question");
				Object answerObj = metadata.get("answer");

				if (!(questionObj instanceof String question) || !(answerObj instanceof String answer)) {
					//log.warn("저장 스킵: question 또는 answer가 null이거나 문자열이 아님. metadata={}", metadata);
					continue;
				}

				int promptTokens = 0;
				int completionTokens = 0;
				int totalTokens = 0;

				Object rawUsage = metadata.get("usage");
				if (rawUsage instanceof Usage usage) {
					promptTokens = usage.getPromptTokens();
					completionTokens = usage.getCompletionTokens();
					totalTokens = usage.getTotalTokens();
				} else {
					//log.warn("usage 정보 없음 또는 형식 불일치. metadata={}", metadata);
				}

				ChatLog chatLog = ChatLog.builder()
					.conversation(conversation)
					.question(question)
					.answer(answer)
					.inputToken(promptTokens)
					.outputToken(completionTokens)
					.totalToken(totalTokens)
					.build();

				//log.info("chatLog 저장 전 호출. 사용자 질문 : {}, LLM 응답 : {}", chatLog.getQuestion(), chatLog.getAnswer());

				conversation.addChatLog(chatLog);
				chatLogRepository.save(chatLog);

			} else if (message instanceof UserMessage userMessage) {
				// metadata가 없거나 question이 없다면 사용자 입력이 아님 (예: LLM이 유추한 메시지)
				if (!message.getMetadata().containsKey("question")) {
					//log.info("유저 입력 아님. 저장 및 출력 생략: {}", message.getText());
					continue;
				}
				//log.info("UserMessage (실제 유저 입력): {}", userMessage.getText());
			}
		}
	}


	/**
	 * 역할: 해당 대화 ID에 해당하는 메시지들을 DB에서 삭제
	 * @param conversationId
	 */
	@Transactional
	@Override
	public void deleteByConversationId(String conversationId) {
		conversationRepository.deleteByConversationId(conversationId);

	}
}
