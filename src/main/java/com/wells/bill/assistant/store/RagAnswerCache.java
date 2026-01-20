package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.RagAnswer;

import java.time.Duration;
import java.util.Optional;

public interface RagAnswerCache {

    Optional<RagAnswer> get(
            String conversationId,
            String billId,
            String normalizedQuestion
    );

    void put(
            String conversationId,
            String billId,
            String normalizedQuestion,
            RagAnswer answer,
            Duration ttl
    );

    void clearConversation(String conversationId);
}
