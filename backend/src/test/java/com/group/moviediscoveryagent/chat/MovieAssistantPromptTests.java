package com.group.moviediscoveryagent.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify that MovieAssistantPrompt defines the correct Markdown contract
 * and that the system prompt enforces clean Markdown formatting.
 */
class MovieAssistantPromptTests {

    @Test
    void systemPromptContainsResponseFormatHeader() {
        assertThat(MovieAssistantPrompt.SYSTEM_PROMPT).contains("RESPONSE FORMAT:");
    }

    @Test
    void systemPromptRequestsCleanMarkdown() {
        assertThat(MovieAssistantPrompt.SYSTEM_PROMPT)
                .contains("Format responses using clean Markdown")
                .contains("short paragraphs")
                .contains("numbered or unordered lists")
                .contains("bold and italic text");
    }

    @Test
    void systemPromptProhibitsRawHtml() {
        assertThat(MovieAssistantPrompt.SYSTEM_PROMPT)
                .contains("Do not return raw HTML");
    }

    @Test
    void systemPromptProhibitsScriptStyleIframeAndFormContent() {
        String prompt = MovieAssistantPrompt.SYSTEM_PROMPT;
        assertThat(prompt)
                .contains("scripts")
                .contains("styles")
                .contains("iframes")
                .contains("forms")
                .contains("images");
    }

    @Test
    void systemPromptRequiresMcpToolsForMovieQueries() {
        assertThat(MovieAssistantPrompt.SYSTEM_PROMPT)
                .contains("use the provided Neon MCP tools");
    }

    @Test
    void systemPromptUsesCorrectColumnNames() {
        String prompt = MovieAssistantPrompt.SYSTEM_PROMPT;
        assertThat(prompt).contains("original_language");
        assertThat(prompt).contains("runtime_minutes");
    }

    @Test
    void systemPromptDoesNotContainFiveRowLimit() {
        // The hard-coded five-result limit should be removed
        assertThat(MovieAssistantPrompt.SYSTEM_PROMPT)
                .doesNotContain("no more than 5")
                .doesNotContain("Return no more than 5")
                .doesNotContain("maximum.*5")
                .doesNotContain("limit.*5");
    }

    @Test
    void systemPromptReturnRequestedNumberOfRecommendations() {
        assertThat(MovieAssistantPrompt.SYSTEM_PROMPT)
                .contains("Return the number of movie recommendations requested by the user");
    }

    @Test
    void systemPromptGuardsAgainstDataExfiltration() {
        String prompt = MovieAssistantPrompt.SYSTEM_PROMPT;
        assertThat(prompt)
                .contains("Do not access conversation")
                .contains("Do not reveal SQL")
                .contains("credentials");
    }
}


