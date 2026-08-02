package com.group.moviediscoveryagent.service;

import com.group.moviediscoveryagent.model.api.MessageResponse;
import com.group.moviediscoveryagent.persistence.entity.Message;
import com.group.moviediscoveryagent.persistence.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;

    public MessageService(MessageRepository messageRepository, ConversationService conversationService) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
    }

    public List<MessageResponse> getMessagesForConversation(UUID conversationId) {
        // ensure conversation exists (will throw NotFoundException if missing)
        var conv = conversationService.findEntity(conversationId);

        List<Message> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return msgs.stream().map(m -> toDto(m)).collect(Collectors.toList());
    }

    private MessageResponse toDto(Message m) {
        var r = new MessageResponse();
        r.setId(m.getId());
        r.setConversationId(m.getConversation().getId());
        r.setRole(m.getRole() == null ? null : m.getRole().name());
        r.setContent(m.getContent());
        r.setToolName(m.getToolName());
        r.setCreatedAt(m.getCreatedAt());
        return r;
    }
}

