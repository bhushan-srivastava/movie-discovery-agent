package com.group.moviediscoveryagent.integration;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Neon MCP integration test.
 *
 * <p>Loads the "development" Spring profile, which supplies the actual Neon
 * MCP Streamable HTTP endpoint and Authorization header from
 * {@code application-development.properties}. This test initializes the
 * Spring AI MCP client, connects to the configured Neon MCP service, and
 * discovers the available tools. It never invokes write or schema-changing
 * MCP tools.</p>
 *
 * <p>Named with the {@code IT} suffix so normal {@code mvn test} execution
 * does not run it automatically.</p>
 */
@SpringBootTest
@ActiveProfiles("development")
class NeonMcpToolDiscoveryIT {

    @Autowired
    private List<McpSyncClient> mcpSyncClients;

    @Value("${spring.ai.mcp.client.streamable-http.connections.neon-mcp.url}")
    private String neonMcpUrl;

    @Value("${spring.ai.mcp.client.streamable-http.connections.neon-mcp.headers.Authorization}")
    private String neonMcpAuthorizationHeader;

    @Test
    void initializesMcpClientForConfiguredNeonMcpEndpoint() {
        assertThat(mcpSyncClients).isNotEmpty();
        assertThat(mcpSyncClients.get(0).isInitialized()).isTrue();
    }

    @Test
    void discoversToolsAndVerifiesQueryCapabilityIsAvailable() {
        assertThat(mcpSyncClients).isNotEmpty();
        McpSyncClient client = mcpSyncClients.get(0);

        McpSchema.ListToolsResult toolsResult = client.listTools();
        List<McpSchema.Tool> tools = toolsResult.tools();

        assertThat(tools).as("Neon MCP should expose at least one tool").isNotEmpty();

        boolean hasQueryCapability = tools.stream()
                .map(McpSchema.Tool::name)
                .filter(name -> name != null)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.contains("query")
                        || name.contains("sql")
                        || name.contains("schema")
                        || name.contains("run")
                        || name.contains("search")
                        || name.contains("fetch"));

        assertThat(hasQueryCapability)
                .as("Expected at least one query/schema-capable MCP tool among: %s",
                        tools.stream().map(McpSchema.Tool::name).toList())
                .isTrue();
    }

    @Test
    void configuredNeonMcpEndpointIsReadOnlyAndProjectScoped() {
        assertThat(neonMcpUrl).contains("mcp.neon.tech");
        assertThat(neonMcpUrl).contains("readonly=true");
        assertThat(neonMcpUrl).contains("category=");

        assertThat(neonMcpAuthorizationHeader).startsWith("Bearer ");
        assertThat(neonMcpAuthorizationHeader.length()).isGreaterThan("Bearer ".length());
    }
}


