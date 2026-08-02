package com.group.moviediscoveryagent.integration;

import com.group.moviediscoveryagent.config.TestChatClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:movie_discovery_gemini;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.ai.google.genai.api-key=test-api-key",
        "spring.ai.google.genai.chat.options.model=gemini-2.5-flash",
        "spring.ai.model.chat=google-genai",
        "spring.ai.mcp.client.enabled=false"
})
@Import(TestChatClientConfig.class)
class GeminiChatClientContextTests {

    @Test
    void loadsGeminiChatClientBuilderBean(@Autowired ChatClient.Builder chatClientBuilder) {
        assertThat(chatClientBuilder).isNotNull();
    }
}




