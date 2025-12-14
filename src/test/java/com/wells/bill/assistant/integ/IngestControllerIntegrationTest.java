package com.wells.bill.assistant.integ;

import com.wells.bill.assistant.service.IngestionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class IngestControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private IngestionService ingestionService;

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        public IngestionService mockIngestionService() {
            return mock(IngestionService.class);
        }
    }

    @Test
    void ingestFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy-content".getBytes()
        );

        Mockito.when(ingestionService.ingestFile(any(UUID.class), any(MultipartFile.class))).thenReturn(4);

        mvc.perform(multipart("/api/ingest/file").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.chunks").value(4))
                .andExpect(jsonPath("$.filename").value("test.pdf"));
    }

    @Test
    void ingestFile_empty_shouldFail() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]
        );

        mvc.perform(multipart("/api/ingest/file").file(empty))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("File cannot be empty"));
    }
}
