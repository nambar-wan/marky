package com.groom.marky.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.groom.marky.domain.ChatLog;
import com.groom.marky.domain.Conversation;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

	List<ChatLog> findTop10ByConversationOrderByCreatedAtAsc(Conversation conversation);

	List<ChatLog> findChatLogsByConversationOrderByCreatedAtAsc(Conversation conversation);

}
