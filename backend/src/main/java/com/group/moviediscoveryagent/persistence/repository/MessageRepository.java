package com.group.moviediscoveryagent.persistence.repository;

import com.group.moviediscoveryagent.persistence.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

	@Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC, m.id ASC")
	List<Message> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") UUID conversationId);

    // Return newest messages first (limit 10) so callers can reverse to chronological order
    List<Message> findTop10ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    // Return newest 2 messages first (used to detect first user message for title update)
    List<Message> findTop2ByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}




