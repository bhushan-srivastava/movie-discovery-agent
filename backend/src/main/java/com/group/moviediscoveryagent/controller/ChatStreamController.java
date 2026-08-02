package com.group.moviediscoveryagent.controller;

import com.group.moviediscoveryagent.chat.ChatOrchestrationService;
import com.group.moviediscoveryagent.chat.ChatRequest;
import com.group.moviediscoveryagent.persistence.repository.ConversationRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.async.DeferredResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/conversations")
public class ChatStreamController {

    private final ConversationRepository conversationRepository;
    private final ChatOrchestrationService orchestrationService;

    // Configurable per NFR-1 ("Model requests SHALL use a configurable timeout").
    // Default (45s) budgets for real Gemini + Neon MCP tool round-trips (MCP
    // request-timeout is 20s) plus model planning/generation latency, while still
    // bounding worst-case wait before the client receives HTTP 504.
    @Value("${app.chat.pre-stream-timeout-ms:45000}")
    private long preStreamTimeoutMs;

    public ChatStreamController(ConversationRepository conversationRepository, ChatOrchestrationService orchestrationService) {
        this.conversationRepository = conversationRepository;
        this.orchestrationService = orchestrationService;
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping(path = "{conversationId}/chat/stream", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<Void> streamChat(@PathVariable("conversationId") UUID conversationId, @RequestBody ChatRequest request, HttpServletResponse response) throws Exception {
        DeferredResult<Void> dr = new DeferredResult<>(preStreamTimeoutMs);
        // Validate conversation exists
        if (!conversationRepository.existsById(conversationId)) {
            response.setStatus(404);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write("{\"status\":404,\"message\":\"Conversation not found\"}".getBytes(StandardCharsets.UTF_8));
            dr.setResult(null);
            return dr;
        }

        // Validate message
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            response.setStatus(400);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write("{\"status\":400,\"message\":\"Message is required\"}".getBytes(StandardCharsets.UTF_8));
            dr.setResult(null);
            return dr;
        }

        // Persist user message (after validation)
        orchestrationService.persistUserMessage(conversationId, request.getMessage());

        // Prepare coordination signals:
        // - firstSignal: completed by orchestration when the model emits its first chunk
        // - writePermit: completed by controller after committing NDJSON headers (ensures headers set before bytes written)
        CompletableFuture<Void> firstSignal = new CompletableFuture<>();
        CompletableFuture<Void> writePermit = new CompletableFuture<>();

        // Start streaming on a background thread so we don't block the servlet thread.
        var future = CompletableFuture.runAsync(() -> {
                try {
                    // The orchestration will write NDJSON to the response OutputStream.
                    orchestrationService.streamAssistantResponse(conversationId, request.getMessage(), response.getOutputStream(), firstSignal, writePermit);
                }
            catch (Exception e) {
                try {
                    // If streaming had already started (firstSignal completed) emit a safe NDJSON error.
                    if (firstSignal.isDone()) {
                        var writer = new com.group.moviediscoveryagent.chat.NdjsonEventWriter();
                        writer.writeEvent(response.getOutputStream(), new com.group.moviediscoveryagent.chat.NdjsonEvent("error", java.util.Collections.singletonMap("message","stream failed")));
                    }
                    else {
                        // no model signal arrived in background; propagate to controller via completing the future exceptionally
                        firstSignal.completeExceptionally(e);
                            // also ensure writePermit is completed so orchestration can exit promptly
                            writePermit.completeExceptionally(e);
                    }
                }
                catch (Exception ex) {
                    // best-effort; nothing else to do
                }
            }
        }, executor);

        // When firstSignal completes successfully, commit response headers for NDJSON and permit writing
        firstSignal.whenComplete((v, t) -> {
            try {
                if (t == null) {
                    response.setStatus(200);
                    response.setContentType("application/x-ndjson;charset=UTF-8");
                    // allow the orchestration to write the first byte(s)
                    writePermit.complete(null);
                }
                else {
                    // model signalled an error before first chunk: respond 504
                    if (!dr.isSetOrExpired()) {
                        try {
                            response.setStatus(504);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getOutputStream().write("{\"status\":504,\"message\":\"Model failed before stream start\"}".getBytes(StandardCharsets.UTF_8));
                        }
                        catch (Exception ignored) {}
                        finally {
                            // let orchestration stop
                            writePermit.completeExceptionally(new RuntimeException("model failure before first chunk"));
                        }
                    }
                }
            }
            catch (Exception e) {
                // ignore header errors
            }
        });

        // On DeferredResult timeout (app.chat.pre-stream-timeout-ms) if firstSignal hasn't completed, respond 504 and cancel background work
        dr.onTimeout(() -> {
            try {
                if (!firstSignal.isDone()) {
                    // cancel background streaming
                    future.cancel(true);
                    response.setStatus(504);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getOutputStream().write("{\"status\":504,\"message\":\"Model did not respond in time\"}".getBytes(StandardCharsets.UTF_8));
                    // signal orchestration that writing is not permitted
                    writePermit.completeExceptionally(new RuntimeException("timeout before first chunk"));
                }
            }
            catch (Exception ignored) {
            }
            finally {
                dr.setResult(null);
            }
        });

        // When background finishes (normal or exceptional) mark the DeferredResult complete
        future.whenComplete((v, t) -> dr.setResult(null));

        return dr;
    }
}







