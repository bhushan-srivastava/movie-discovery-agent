package com.group.moviediscoveryagent.service;

import com.group.moviediscoveryagent.model.api.CreateConversationRequest;
import com.group.moviediscoveryagent.persistence.entity.Conversation;
import com.group.moviediscoveryagent.persistence.repository.ConversationRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConversationServiceTest {

    @Test
    void createConversation_defaultsTitleAndSetsTimestamps() {
        var repo = mock(ConversationRepository.class);
        when(repo.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        var svc = new ConversationService(repo);
        var req = new CreateConversationRequest();
        req.setTitle("   ");

        var resp = svc.createConversation(req);

        assertThat(resp.getTitle()).isEqualTo("New conversation");
        assertThat(resp.getId()).isNotNull();
        assertThat(resp.getCreatedAt()).isNotNull();
        assertThat(resp.getUpdatedAt()).isNotNull();
        assertThat(resp.getCreatedAt()).isEqualTo(resp.getUpdatedAt());
    }
}

