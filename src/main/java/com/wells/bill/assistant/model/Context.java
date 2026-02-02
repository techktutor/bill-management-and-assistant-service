package com.wells.bill.assistant.model;

import java.util.UUID;

public record Context(UUID contextId, UUID conversationId, UUID userId) {

}
