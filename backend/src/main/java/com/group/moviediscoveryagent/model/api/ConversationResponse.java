package com.group.moviediscoveryagent.model.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ConversationResponse {
    private UUID id;
    private String title;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ConversationResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

