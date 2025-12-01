package com.wells.bill.assistant.integ;

import com.wells.bill.assistant.service.RagEngineService;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RagQueryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RagEngineService ragEngineService;  // ← get injected mock bean

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        public RagEngineService mockRagEngineService() {
            return mock(RagEngineService.class);
        }
    }

    @Test
    void testAnswerRagQuery() throws Exception {

        // stub the injected mock
        Mockito.when(ragEngineService.answerQuestionForBill("1", "why?"))
                .thenReturn("Because...");

        mockMvc.perform(get("/api/rag/answerBillQuery")
                        .param("billId", "1")
                        .param("question", "why?")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId").value("1"))
                .andExpect(jsonPath("$.question").value("why?"))
                .andExpect(jsonPath("$.answer").value("Because..."));
    }

    @Test
    void testMissingBillId() throws Exception {
        mockMvc.perform(get("/api/rag/answerBillQuery")
                        .param("q", "test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("billId is required"));
    }

    @Test
    void testMissingQuestion() throws Exception {
        mockMvc.perform(get("/api/rag/answerBillQuery")
                        .param("billId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("question is required"));
    }
}
