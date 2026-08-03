package com.group.moviediscoveryagent.controller;

import com.group.moviediscoveryagent.chat.ChatOrchestrationService;
import com.group.moviediscoveryagent.chat.ChatRequest;
import com.group.moviediscoveryagent.exception.NotFoundException;
import com.group.moviediscoveryagent.model.api.ChatResponse;
import com.group.moviediscoveryagent.persistence.repository.ConversationRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ChatController {

    private final ConversationRepository conversationRepository;
    private final ChatOrchestrationService orchestrationService;

    public ChatController(ConversationRepository conversationRepository, ChatOrchestrationService orchestrationService) {
        this.conversationRepository = conversationRepository;
        this.orchestrationService = orchestrationService;
    }

    @PostMapping(path = "{conversationId}/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> chat(@PathVariable("conversationId") UUID conversationId, @RequestBody(required = false) ChatRequest request) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new NotFoundException("Conversation not found");
        }
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required");
        }
        orchestrationService.persistUserMessage(conversationId, request.getMessage());
        return ResponseEntity.ok(new ChatResponse(orchestrationService.completeAssistantResponse(conversationId, request.getMessage())));
    }
}








