package com.group.moviediscoveryagent.chat;

import com.group.moviediscoveryagent.persistence.entity.Conversation;
import com.group.moviediscoveryagent.persistence.entity.Message;
import com.group.moviediscoveryagent.persistence.entity.MessageRole;
import com.group.moviediscoveryagent.persistence.repository.ConversationRepository;
import com.group.moviediscoveryagent.persistence.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ChatOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestrationService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatClient chatClient;
    private final List<ToolCallbackProvider> toolCallbackProviders;

    public ChatOrchestrationService(ConversationRepository conversationRepository, MessageRepository messageRepository,
                                    ChatClient chatClient,
                                    List<ToolCallbackProvider> toolCallbackProviders) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.chatClient = chatClient;
        this.toolCallbackProviders = toolCallbackProviders;

        // Log MCP tool provider presence and count on startup
        if (toolCallbackProviders != null && !toolCallbackProviders.isEmpty()) {
            log.info("MCP_TOOLS enabled with {} provider(s); tools will be available to ChatClient requests", toolCallbackProviders.size());
        } else {
            log.warn("MCP_TOOLS no ToolCallbackProvider configured; MCP tools unavailable");
        }
    }

    public Conversation findConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId).orElse(null);
    }

    @Transactional
    public Message persistUserMessage(UUID conversationId, String content) {
        String correlationId = UUID.randomUUID().toString();
        log.info("AI_REQUEST conversationId={} requestId={} messageLength={}", conversationId, correlationId, content.length());

        Conversation conv = conversationRepository.findById(conversationId).orElseThrow();
        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setConversation(conv);
        msg.setRole(MessageRole.USER);
        msg.setContent(content);
        msg.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return messageRepository.save(msg);
    }

    public List<Message> loadLatest10Chronological(UUID conversationId) {
        List<Message> latest = messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(conversationId);
        if (latest == null || latest.isEmpty()) return Collections.emptyList();
        Collections.reverse(latest); // now chronological oldest->newest
        log.debug("AI_CONTEXT conversationId={} contextSize={}", conversationId, latest.size());
        return latest;
    }

    @Transactional
    public void persistAssistantMessageAndUpdateConversation(UUID conversationId, String assistantContent) {
        Conversation conv = conversationRepository.findById(conversationId).orElseThrow();
        Message assistant = new Message();
        assistant.setId(UUID.randomUUID());
        assistant.setConversation(conv);
        assistant.setRole(MessageRole.ASSISTANT);
        assistant.setContent(assistantContent);
        assistant.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        messageRepository.save(assistant);
        conv.setUpdatedAt(assistant.getCreatedAt());
        conversationRepository.save(conv);

        log.info("AI_RESPONSE conversationId={} responseLength={}", conversationId, assistantContent.length());

        // Update title if this is the first user message response
        if ("New conversation".equals(conv.getTitle())) {
            List<Message> latest = messageRepository.findTop2ByConversationIdOrderByCreatedAtDesc(conversationId);
            if (latest != null && latest.size() >= 2) {
                Message previousMsg = latest.get(1); // older of the two
                if (previousMsg.getRole() == MessageRole.USER) {
                    String newTitle = previousMsg.getContent().trim();
                    if (!newTitle.isEmpty()) {
                        if (newTitle.length() > 255) {
                            newTitle = newTitle.substring(0, 255);
                        }
                        conv.setTitle(newTitle);
                        conversationRepository.save(conv);
                        log.info("CONVERSATION_TITLE_UPDATED conversationId={} titleLength={}", conversationId, newTitle.length());
                    }
                }
            }
        }
    }

    /** Executes one completed model request and persists its assistant message on success only. */
    @Transactional
    public String completeAssistantResponse(UUID conversationId, String userMessage) {
        List<Message> context = loadLatest10Chronological(conversationId);
        var reqSpec = chatClient.prompt().system(MovieAssistantPrompt.SYSTEM_PROMPT);
        for (Message m : context) {
            if (m.getRole() == MessageRole.USER) {
                reqSpec.user(m.getContent());
            } else if (m.getRole() == MessageRole.ASSISTANT) {
                reqSpec.system("Assistant: " + m.getContent());
            }
        }
        if (context.isEmpty() || !userMessage.equals(context.get(context.size() - 1).getContent())) {
            reqSpec.user(userMessage);
        }
        if (toolCallbackProviders != null && !toolCallbackProviders.isEmpty()) {
            reqSpec.tools(toolCallbackProviders.toArray());
        }
        String assistantContent = reqSpec.call().content();
        if (assistantContent == null || assistantContent.isBlank()) {
            throw new IllegalStateException("The assistant returned an empty response");
        }
        persistAssistantMessageAndUpdateConversation(conversationId, assistantContent);
        return assistantContent;
    }
}
