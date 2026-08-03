package com.group.moviediscoveryagent.config;

import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test configuration that provides a mocked ChatClient for integration tests.
 * This prevents the application context from failing due to missing ChatModel beans
 * and provides a deterministic completed response for tests.
 */
@TestConfiguration
public class TestChatClientConfig {

    @Bean
    @Primary
    public ChatClient chatClient() {
        ChatClient mockClient = Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);

        // Create a mock request spec that returns a completed response
        ChatClient.ChatClientRequestSpec mockRequestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class, Mockito.RETURNS_SELF);
        ChatClient.CallResponseSpec mockCallSpec = Mockito.mock(ChatClient.CallResponseSpec.class);

        // When prompt() is called with no args, with a string, or with a Prompt, return the mock request spec
        when(mockClient.prompt()).thenReturn(mockRequestSpec);
        when(mockClient.prompt(anyString())).thenReturn(mockRequestSpec);
        when(mockClient.prompt(any(Prompt.class))).thenReturn(mockRequestSpec);

        // The request spec returns itself for all these calls (for chaining)
        when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.system(anyString())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.tools(any())).thenReturn(mockRequestSpec);

        when(mockRequestSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.content()).thenReturn("Hello from test model");

        return mockClient;
    }
}




