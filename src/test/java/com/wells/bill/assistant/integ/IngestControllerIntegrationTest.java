package com.wells.bill.assistant.integ;

import com.wells.bill.assistant.model.BillDetails;
import com.wells.bill.assistant.service.TextExtractorService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IngestControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    IngestionService mockIngestionService;

    @Autowired
    TextExtractorService mockTextExtractorService;

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        public IngestionService mockIngestionService() {
            return mock(IngestionService.class);
        }

        @Bean
        @Primary
        public TextExtractorService mockBillTextExtractionService() {
            return mock(TextExtractorService.class);
        }
    }

    @Test
    void ingestFile_success() throws Exception {
        MockMultipartFile file = getMockMultipartFile();

        String normalizedText = """
                STATE ELECTRICITY DISTRIBUTION COMPANY LTD Electricity Bill (Tax Invoice) Consumer Name : Ramesh Kumar Consumer Number : 12345678901 Service Connection : Domestic Billing Period : 01-Aug-2025 to 31-Aug-2025 Meter Number : DL123456 Previous Reading : 2548.00 Current Reading : 2675.00 Units Consumed : 127 kWh Energy Charges : Rs. 635.00 Fixed Charges : Rs. 120.00 Electricity Duty : Rs. 45.00 Fuel Adjustment : Rs. 18.50 Late Payment Surcharge (if any) : Rs. 25.00 ----------------------------------------- Total Amount Due : ₹ 818.50 ----------------------------------------- Bill Issue Date : 05-Aug-2025 Due Date : 20-Aug-2025 Last Due Date : 25-Aug-2025 After Due Date, a Late Payment Surcharge will be applicable as per tariff order. Customer Care: Toll Free No : 1912 Website : www.statepower.in This is a computer generated bill.
                """;
        Mockito.when(mockTextExtractorService.extractText(any(MultipartFile.class))).thenReturn(normalizedText);

        BillDetails details = new BillDetails();
        details.setAmount(new BigDecimal("818.50")); // Simulate missing amount to trigger LLM extraction
        details.setDueDate(LocalDate.now()); // Simulate missing due date to trigger LLM extraction
        details.setConsumerName("Ramesh Kumar");
        details.setConsumerNumber("12345678901");
        Mockito.when(mockTextExtractorService.extractUsingLLM(anyString())).thenReturn(details);

        Mockito.when(mockIngestionService.ingestFile(any(UUID.class), any(MultipartFile.class))).thenReturn(4);

        mvc.perform(multipart("/api/ingest/file").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.chunks").value(4))
                .andExpect(jsonPath("$.filename").value("test.pdf"));
    }

    private static MockMultipartFile getMockMultipartFile() {
        String content = """
                                       STATE ELECTRICITY DISTRIBUTION COMPANY LTD
                                       Electricity Bill (Tax Invoice)
                
                                       Consumer Name       : Ramesh Kumar
                                       Consumer Number     : 12345678901
                                       Service Connection  : Domestic
                                       Billing Period      : 01-Aug-2025 to 31-Aug-2025
                
                                       Meter Number        : DL123456
                                       Previous Reading    : 2548.00
                                       Current Reading     : 2675.00
                                       Units Consumed      : 127 kWh
                
                                       Energy Charges      : Rs. 635.00
                                       Fixed Charges       : Rs. 120.00
                                       Electricity Duty    : Rs. 45.00
                                       Fuel Adjustment     : Rs. 18.50
                                       Late Payment Surcharge (if any) : Rs. 25.00
                
                                       -----------------------------------------
                                       Total Amount Due    : ₹ 818.50
                                       -----------------------------------------
                
                                       Bill Issue Date     : 05-Aug-2025
                                       Due Date            : 20-Aug-2025
                                       Last Due Date       : 25-Aug-2025
                
                                       After Due Date, a Late Payment Surcharge
                                       will be applicable as per tariff order.
                
                                       Customer Care:
                                       Toll Free No        : 1912
                                       Website             : www.statepower.in
                
                                       This is a computer generated bill.
                """;

        return new MockMultipartFile(
                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, content.getBytes()
        );
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
