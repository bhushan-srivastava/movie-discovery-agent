package com.group.moviediscoveryagent.persistence.repository;

import com.group.moviediscoveryagent.persistence.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
}

