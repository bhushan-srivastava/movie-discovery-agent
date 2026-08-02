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

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ChatOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestrationService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NdjsonEventWriter writer = new NdjsonEventWriter();
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

    /**
     * Start a streaming assistant response that writes NDJSON events to the provided OutputStream.
     * This implementation uses the installed Spring AI 2.0 ChatClient streaming API.
     */
    public void streamAssistantResponse(UUID conversationId, String userMessage, OutputStream out) throws Exception {
        CompletableFuture<Void> firstSignal = new CompletableFuture<>();
        CompletableFuture<Void> writePermit = new CompletableFuture<>();
        firstSignal.whenComplete((ignored, failure) -> {
            if (failure == null) {
                writePermit.complete(null);
            }
            else {
                writePermit.completeExceptionally(failure);
            }
        });
        streamAssistantResponse(conversationId, userMessage, out, firstSignal, writePermit);
    }

    /**
     * Non-blocking streaming entrypoint with explicit signals.
     */
    public void streamAssistantResponse(UUID conversationId, String userMessage, OutputStream out,
                                        CompletableFuture<Void> firstSignal,
                                        CompletableFuture<Void> writePermit) throws Exception {
        String streamId = UUID.randomUUID().toString();
        log.info("STREAM_START conversationId={} streamId={}", conversationId, streamId);

        // Load context
        List<Message> context = loadLatest10Chronological(conversationId);

        // Prepare a queue to receive NDJSON lines from the model stream
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        AtomicBoolean firstSeen = new AtomicBoolean(false);

        // Accumulate assistant content so we can persist the full assistant message once complete
        StringBuilder assistantAccumulator = new StringBuilder();

        // Build chat request using the ChatClient fluent API.
        var reqSpec = chatClient.prompt()
                 .system(MovieAssistantPrompt.SYSTEM_PROMPT);

        // Add historical messages as repeated user/system calls
        for (Message m : context) {
            if (m.getRole() == MessageRole.USER) {
                reqSpec.user(m.getContent());
            }
            else if (m.getRole() == MessageRole.ASSISTANT) {
                reqSpec.system("Assistant: " + m.getContent());
            }
        }

        // Add current user message
        reqSpec.user(userMessage);

        // Attach tool callback providers if available
        if (toolCallbackProviders != null && !toolCallbackProviders.isEmpty()) {
            log.debug("MCP_TOOLS supplied to ChatClient request; {} tool provider(s) available", toolCallbackProviders.size());
            reqSpec.tools(toolCallbackProviders.toArray());
        }

        // Subscribe to the streaming content flux
        var streamSpec = reqSpec.stream();
        var contentFlux = streamSpec.content();

        var subscription = contentFlux.subscribe(
                chunk -> {
                    try {
                        if (firstSeen.compareAndSet(false, true)) {
                            log.debug("STREAM_FIRST_CHUNK streamId={}", streamId);
                            firstSignal.complete(null);
                        }
                        assistantAccumulator.append(chunk);
                        String ndjson = writer.toJson(new NdjsonEvent("text-delta", Collections.singletonMap("delta", chunk)));
                        queue.put(ndjson);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    catch (Exception e) {
                        try {
                            queue.put("__STREAM_END__");
                        }
                        catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                },
                error -> {
                    try {
                        log.error("STREAM_ERROR streamId={} error={} message={}", streamId, error.getClass().getSimpleName(), error.getMessage());
                        if (!firstSignal.isDone()) {
                            firstSignal.completeExceptionally(error);
                        }
                        // emit a safe NDJSON error event with detailed error info
                        String errorMsg = error.getMessage() != null ? error.getMessage() : "model stream failed";
                        // Sanitize error message to prevent JSON parsing issues
                        errorMsg = errorMsg.replace("\"", "'");
                        String err = writer.toJson(new NdjsonEvent("error", Collections.singletonMap("message", errorMsg)));
                        queue.put(err);
                        queue.put("__STREAM_END__");
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    catch (Exception e) {
                        try {
                            queue.put("__STREAM_END__");
                        }
                        catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                },
                () -> {
                    try {
                        log.info("STREAM_COMPLETE streamId={} accumulatedLength={}", streamId, assistantAccumulator.length());
                        // Persist only after successful completion
                        persistAssistantMessageAndUpdateConversation(conversationId, assistantAccumulator.toString());
                        // emit completion event
                        var completionData = Collections.<String, Object>singletonMap("message", "done");
                        String done = writer.toJson(new NdjsonEvent("completion", completionData));
                        queue.put(done);
                        queue.put("__STREAM_END__");
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    catch (Exception e) {
                        try {
                            queue.put("__STREAM_END__");
                        }
                        catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
        );

        // Drain queue and write to provided OutputStream until sentinel
        try {
            boolean wroteFirst = false;
            while (true) {
                String line = queue.take();
                if ("__STREAM_END__".equals(line)) break;
                // ensure controller has set headers and permitted writing after first model signal
                if (!wroteFirst) {
                    try {
                        writePermit.join();
                    }
                    catch (Exception joinEx) {
                        // controller signalled failure/timeout; stop without emitting partial persistence
                        break;
                    }
                    wroteFirst = true;
                }
                writer.writeRawLine(out, line);
            }
        }
        finally {
            subscription.dispose();
        }
    }
}
