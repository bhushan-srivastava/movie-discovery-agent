package com.group.moviediscoveryagent.controller;

import com.group.moviediscoveryagent.model.api.ConversationResponse;
import com.group.moviediscoveryagent.model.api.CreateConversationRequest;
import com.group.moviediscoveryagent.model.api.MessageResponse;
import com.group.moviediscoveryagent.service.ConversationService;
import com.group.moviediscoveryagent.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService, MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(@RequestBody CreateConversationRequest req) {
        var created = conversationService.createConversation(req);
        var location = URI.create("/api/conversations/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<ConversationResponse> listConversations() {
        return conversationService.listConversations();
    }

    @GetMapping("/{id}")
    public ConversationResponse getConversation(@PathVariable("id") UUID id) {
        return conversationService.getConversation(id);
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> getMessages(@PathVariable("id") UUID id) {
        return messageService.getMessagesForConversation(id);
    }
}

