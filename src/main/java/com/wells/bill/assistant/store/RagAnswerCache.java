package com.wells.bill.assistant.store;

import com.wells.bill.assistant.service.RagEngineService;

import java.time.Duration;
import java.util.Optional;

public interface RagAnswerCache {

    Optional<RagEngineService.RagAnswer> get(
            String conversationId,
            String billId,
            String normalizedQuestion
    );

    void put(
            String conversationId,
            String billId,
            String normalizedQuestion,
            RagEngineService.RagAnswer answer,
            Duration ttl
    );

    void clearConversation(String conversationId);
}
