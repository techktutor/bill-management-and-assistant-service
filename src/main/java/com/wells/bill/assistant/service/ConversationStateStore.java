package com.wells.bill.assistant.service;

import java.time.Duration;

public interface ConversationStateStore {

    String get(String conversationId, String key);

    void put(String conversationId, String key, String value, Duration ttl);

    void clear(String conversationId, String key);
}
