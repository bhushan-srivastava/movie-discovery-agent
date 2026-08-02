package com.group.moviediscoveryagent.service;

import com.group.moviediscoveryagent.exception.NotFoundException;
import com.group.moviediscoveryagent.model.api.ConversationResponse;
import com.group.moviediscoveryagent.model.api.CreateConversationRequest;
import com.group.moviediscoveryagent.persistence.entity.Conversation;
import com.group.moviediscoveryagent.persistence.repository.ConversationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public ConversationResponse createConversation(CreateConversationRequest req) {
        var title = req == null || req.getTitle() == null ? "" : req.getTitle().trim();
        if (title.isEmpty()) {
            title = "New conversation";
        }

        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var conv = new Conversation();
        conv.setId(UUID.randomUUID());
        conv.setTitle(title);
        conv.setCreatedAt(now);
        conv.setUpdatedAt(now);

        var saved = conversationRepository.save(conv);
        return toDto(saved);
    }

    public List<ConversationResponse> listConversations() {
        var list = conversationRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ConversationResponse getConversation(UUID id) {
        var conv = conversationRepository.findById(id).orElseThrow(() -> new NotFoundException("Conversation not found"));
        return toDto(conv);
    }

    public Conversation findEntity(UUID id) {
        return conversationRepository.findById(id).orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    private ConversationResponse toDto(Conversation c) {
        var r = new ConversationResponse();
        r.setId(c.getId());
        r.setTitle(c.getTitle());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        return r;
    }
}

