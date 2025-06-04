package com.groom.marky.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.groom.marky.domain.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

	@Query("select c.conversationId from Conversation c")
	List<String> findAllConversationIds();

	Optional<Conversation> findConversationByConversationId(String conversationId);

	void deleteByConversationId(String conversationId);
}
