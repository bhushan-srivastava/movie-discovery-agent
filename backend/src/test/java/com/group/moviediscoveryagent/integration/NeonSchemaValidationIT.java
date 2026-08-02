package com.group.moviediscoveryagent.integration;

import com.group.moviediscoveryagent.persistence.repository.ConversationRepository;
import com.group.moviediscoveryagent.persistence.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Neon integration test.
 *
 * <p>Loads the "development" Spring profile, which supplies actual Neon
 * connection settings from {@code application-development.properties}.
 * With {@code spring.jpa.hibernate.ddl-auto=validate}, Hibernate validates
 * the {@code Conversation} and {@code Message} entity mappings against the
 * existing Neon schema at context startup. No schema objects are created,
 * altered, or migrated by this test or by the application.</p>
 *
 * <p>Named with the {@code IT} suffix so normal {@code mvn test} execution
 * does not run it automatically.</p>
 */
@SpringBootTest
@ActiveProfiles("development")
class NeonSchemaValidationIT {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void contextLoadsAndValidatesConversationAndMessageMappingsAgainstNeon() {
        assertThat(conversationRepository).isNotNull();
        assertThat(messageRepository).isNotNull();
    }
}

