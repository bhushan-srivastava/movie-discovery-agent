package com.group.moviediscoveryagent.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}


