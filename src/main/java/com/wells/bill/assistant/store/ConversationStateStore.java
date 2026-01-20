package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.ConversationContext;

import java.time.Duration;

public interface ConversationStateStore {

    ConversationContext load(String conversationId, String userId);

    void save(ConversationContext context, Duration ttl);

    void delete(String conversationId);
}
