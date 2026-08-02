package com.group.moviediscoveryagent.persistence;

import com.group.moviediscoveryagent.config.TestChatClientConfig;
import com.group.moviediscoveryagent.persistence.entity.Conversation;
import com.group.moviediscoveryagent.persistence.entity.Message;
import com.group.moviediscoveryagent.persistence.entity.MessageRole;
import com.group.moviediscoveryagent.persistence.repository.ConversationRepository;
import com.group.moviediscoveryagent.persistence.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:movie_discovery_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.ai.model.chat=none",
        "spring.ai.mcp.client.enabled=false"
})
@Import(TestChatClientConfig.class)
class ConversationMessageRepositoryTests {

    @Test
    void savesConversationAndMessageUsingMappedColumns(
            @Autowired ConversationRepository conversationRepository,
            @Autowired MessageRepository messageRepository
    ) {
        var clock = Clock.system(ZoneOffset.UTC);

        var conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setTitle("Weekend movies");
        conversation.setCreatedAt(OffsetDateTime.now(clock));
        conversation.setUpdatedAt(OffsetDateTime.now(clock));
        conversationRepository.save(conversation);

        var message = new Message();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setRole(MessageRole.USER);
        message.setContent("Recommend sci-fi classics");
        message.setToolName("search_movies");
        message.setCreatedAt(OffsetDateTime.now(clock));
        messageRepository.save(message);

        assertThat(conversationRepository.findById(conversation.getId())).isPresent();
        var persistedMessage = messageRepository.findById(message.getId());
        assertThat(persistedMessage).isPresent();
        assertThat(persistedMessage.get().getConversation().getId()).isEqualTo(conversation.getId());
    }
}






