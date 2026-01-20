package com.wells.bill.assistant.integ;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wells.bill.assistant.model.ChatRequest;
import com.wells.bill.assistant.service.OrchestratorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OrchestratorService orchestrator; // mock injected here

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary // override real bean
        public OrchestratorService mockOrchestrator() {
            return mock(OrchestratorService.class);
        }
    }

    @Test
    void testChat() throws Exception {
        Mockito.when(orchestrator.processMessage(ArgumentMatchers.any(ChatRequest.class)))
                .thenReturn("Hi there!");

        ChatRequest req = new ChatRequest("c1", "hello");

        mvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hi there!"));
    }

    @Test
    void testHealth() throws Exception {
        mvc.perform(get("/api/chat/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Chat service is healthy"));
    }
}

