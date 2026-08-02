package com.group.moviediscoveryagent.mcp.client;

import org.springframework.ai.mcp.client.webflux.transport.WebClientStreamableHttpTransport;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Attaches the configured Authorization header to the Neon MCP Streamable
 * HTTP connection.
 *
 * <p>Spring AI's {@code spring.ai.mcp.client.streamable-http.connections.*}
 * properties only bind {@code url} and {@code endpoint}; there is no
 * property-bound {@code headers} map for this transport. The
 * {@code spring.ai.mcp.client.streamable-http.connections.neon-mcp.headers.Authorization}
 * value is still read directly from the Spring {@code Environment} here and
 * applied to the underlying {@link WebClient} used for the "neon-mcp"
 * connection via {@link McpClientCustomizer}.</p>
 */
@Configuration
public class NeonMcpClientConfig {

    private static final String NEON_MCP_CONNECTION_NAME = "neon-mcp";

    @Bean
    @ConditionalOnProperty(
            prefix = "spring.ai.mcp.client.streamable-http.connections.neon-mcp.headers",
            name = "Authorization")
    public McpClientCustomizer<WebClientStreamableHttpTransport.Builder> neonMcpAuthorizationHeaderCustomizer(
            @Value("${spring.ai.mcp.client.streamable-http.connections.neon-mcp.url:}") String neonMcpUrl,
            @Value("${spring.ai.mcp.client.streamable-http.connections.neon-mcp.headers.Authorization:}")
            String neonMcpAuthorizationHeader) {
        return (connectionName, builder) -> {
            if (NEON_MCP_CONNECTION_NAME.equals(connectionName)) {
                builder.webClientBuilder(WebClient.builder()
                        .baseUrl(neonMcpUrl)
                        .defaultHeader(HttpHeaders.AUTHORIZATION, neonMcpAuthorizationHeader));
            }
        };
    }
}

