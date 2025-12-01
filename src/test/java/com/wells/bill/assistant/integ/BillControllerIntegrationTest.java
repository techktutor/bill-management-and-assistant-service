package com.wells.bill.assistant.integ;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.model.BillCreateRequest;
import com.wells.bill.assistant.model.BillUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class BillControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void testCreateGetUpdateDeleteBill() throws Exception {
        BillCreateRequest req = new BillCreateRequest();
        req.setCustomerId(UUID.randomUUID());
        req.setName("Electricity");
        req.setAmount(new BigDecimal("120.50"));
        req.setDueDate(LocalDate.now().plusDays(5));

        // CREATE
        String response = mvc.perform(post("/api/bills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        BillEntity created = mapper.readValue(response, BillEntity.class);

        // GET
        mvc.perform(get("/api/bills/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electricity"));

        // UPDATE
        BillUpdateRequest upd = new BillUpdateRequest();
        upd.setName("Electricity Updated");
        upd.setAmount(new BigDecimal("140.00"));
        upd.setDueDate(LocalDate.now().plusDays(10));

        mvc.perform(put("/api/bills/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electricity Updated"));

        // DELETE
        mvc.perform(delete("/api/bills/" + created.getId()))
                .andExpect(status().isNoContent());
    }
}

