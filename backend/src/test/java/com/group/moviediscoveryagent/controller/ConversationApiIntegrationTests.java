package com.group.moviediscoveryagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group.moviediscoveryagent.config.TestChatClientConfig;
import com.group.moviediscoveryagent.persistence.entity.Conversation;
import com.group.moviediscoveryagent.persistence.entity.Message;
import com.group.moviediscoveryagent.persistence.entity.MessageRole;
import com.group.moviediscoveryagent.persistence.repository.ConversationRepository;
import com.group.moviediscoveryagent.persistence.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestChatClientConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:movie_discovery_api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.ai.model.chat=none",
        "spring.ai.mcp.client.enabled=false"
})
class ConversationApiIntegrationTests {

    private MockMvc mvc;

    @Autowired
    private WebApplicationContext wac;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }
    @Test
    @Transactional
    void postCreatesConversation_defaultsBlankTitle() throws Exception {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        var payload = mapper.createObjectNode();
        payload.put("title", "   ");

        var result = mvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New conversation"))
                .andReturn();

        var resp = mapper.readTree(result.getResponse().getContentAsString());
        UUID id = UUID.fromString(resp.get("id").asText());
        assertThat(conversationRepository.findById(id)).isPresent();
    }

    @Test
    @Transactional
    void listConversations_returns200() throws Exception {
        var conv = new Conversation();
        conv.setId(UUID.randomUUID());
        conv.setTitle("List Test");
        conv.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        conv.setUpdatedAt(conv.getCreatedAt());
        conversationRepository.save(conv);

        mvc.perform(get("/api/conversations")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty());
    }

    @Test
    @Transactional
    void getConversation_returns200_whenFound() throws Exception {
        var conv = new Conversation();
        conv.setId(UUID.randomUUID());
        conv.setTitle("GetOne");
        conv.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        conv.setUpdatedAt(conv.getCreatedAt());
        conversationRepository.save(conv);

        mvc.perform(get("/api/conversations/" + conv.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conv.getId().toString()));
    }

    @Test
    void getConversation_returns404_whenMissing() throws Exception {
        mvc.perform(get("/api/conversations/" + UUID.randomUUID())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @Transactional
    void getMessages_returnsOrderedMessages_forConversation() throws Exception {
        var conv = new Conversation();
        conv.setId(UUID.randomUUID());
        conv.setTitle("Msgs");
        conv.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        conv.setUpdatedAt(conv.getCreatedAt());
        conversationRepository.save(conv);

        var m1 = new Message();
        m1.setId(UUID.randomUUID());
        m1.setConversation(conv);
        m1.setRole(MessageRole.USER);
        m1.setContent("first");
        m1.setCreatedAt(OffsetDateTime.of(2020,1,1,0,0,1,0, ZoneOffset.UTC));
        messageRepository.save(m1);

        var m2 = new Message();
        m2.setId(UUID.randomUUID());
        m2.setConversation(conv);
        m2.setRole(MessageRole.ASSISTANT);
        m2.setContent("second");
        m2.setCreatedAt(OffsetDateTime.of(2020,1,1,0,0,2,0, ZoneOffset.UTC));
        messageRepository.save(m2);

        mvc.perform(get("/api/conversations/" + conv.getId() + "/messages")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("first"))
                .andExpect(jsonPath("$[1].content").value("second"));
    }

    @Test
    void getMessages_returns404_whenConversationMissing() throws Exception {
        mvc.perform(get("/api/conversations/" + UUID.randomUUID() + "/messages")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}






