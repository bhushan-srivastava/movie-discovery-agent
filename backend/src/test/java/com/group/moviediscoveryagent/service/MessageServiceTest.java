package com.group.moviediscoveryagent.service;

import com.group.moviediscoveryagent.persistence.entity.Conversation;
import com.group.moviediscoveryagent.persistence.entity.Message;
import com.group.moviediscoveryagent.persistence.entity.MessageRole;
import com.group.moviediscoveryagent.persistence.repository.MessageRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageServiceTest {

    @Test
    void getMessagesForConversation_mapsAndOrders() {
        var repo = mock(MessageRepository.class);
        var convSvc = mock(ConversationService.class);

        var conv = new Conversation();
        conv.setId(UUID.randomUUID());
        when(convSvc.findEntity(conv.getId())).thenReturn(conv);

        var m1 = new Message();
        m1.setId(UUID.randomUUID());
        m1.setConversation(conv);
        m1.setRole(MessageRole.USER);
        m1.setContent("first");
        m1.setCreatedAt(OffsetDateTime.of(2020,1,1,0,0,0,0, ZoneOffset.UTC));

        var m2 = new Message();
        m2.setId(UUID.randomUUID());
        m2.setConversation(conv);
        m2.setRole(MessageRole.ASSISTANT);
        m2.setContent("second");
        m2.setCreatedAt(OffsetDateTime.of(2020,1,1,0,0,1,0, ZoneOffset.UTC));

        when(repo.findByConversationIdOrderByCreatedAtAsc(conv.getId())).thenReturn(List.of(m1, m2));

        var svc = new MessageService(repo, convSvc);
        var result = svc.getMessagesForConversation(conv.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("first");
        assertThat(result.get(1).getContent()).isEqualTo("second");
        assertThat(result.get(0).getConversationId()).isEqualTo(conv.getId());
    }
}

