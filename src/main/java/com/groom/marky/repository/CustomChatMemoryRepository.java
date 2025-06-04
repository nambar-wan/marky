package com.groom.marky.repository;

import java.util.List;

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
	private final UserRepository userRepository;

	@Autowired
	public CustomChatMemoryRepository(ChatLogRepository chatLogRepository,
		ConversationRepository conversationRepository,
		UserRepository userRepository) {
		this.chatLogRepository = chatLogRepository;
		this.conversationRepository = conversationRepository;
		this.userRepository = userRepository;
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

		log.info("[CustomChatMemoryRepository] saveAll 진입. conversationId : {} ", conversationId);

		Conversation conversation = conversationRepository.findConversationByConversationId(conversationId)
			.orElse(null);

		if (conversation == null) {
			log.warn("저장 스킵: conversationId {} 에 해당하는 Conversation이 없음", conversationId);
			return;
		}

		log.warn("message size: {}", messages.size());

		for (Message message : messages) {
			log.warn("메시지 타입: {}", message.getClass().getSimpleName());
			log.warn("내용: {}", message.getText());

			if (message instanceof AssistantMessage) {

				String question = (String) message.getMetadata().get("question");
				String answer = (String) message.getMetadata().get("answer");
				Object rawUsage = message.getMetadata().get("usage");


				int promptTokens = 0;
				int completionTokens = 0;
				int totalTokens = 0;


				if (rawUsage instanceof Usage usage) {
					promptTokens = usage.getPromptTokens();
					completionTokens = usage.getCompletionTokens();
					totalTokens = usage.getTotalTokens();
				}

				ChatLog chatLog = ChatLog.builder()
					.conversation(conversation)
					.question(question)
					.answer(answer)
					.inputToken(promptTokens)
					.outputToken(completionTokens)
					.totalToken(totalTokens)
					.build();

				log.warn("저장 전: {}", chatLog);

				conversation.addChatLog(chatLog);
				chatLogRepository.save(chatLog);

				/**
				 * TODO : 유저 테이블에 사용량 갱신
				 */
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
