package com.wells.bill.assistant.store;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Repository("jdbcConversationStateStore")
public class JdbcConversationStateStore implements ConversationStateStore {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcConversationStateStore(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public void put(String conversationId, String key, String value, Duration ttl) {
        Instant acknowledge = clock.instant();
        Instant expiresAt = acknowledge.plus(ttl);

        jdbcTemplate.update("""
                        INSERT INTO conversation_state
                            (conversation_id, state_key, state_value, expires_at)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (conversation_id, state_key)
                        DO UPDATE SET
                            state_value = EXCLUDED.state_value,
                            expires_at  = EXCLUDED.expires_at
                        """,
                conversationId,
                key,
                value,
                Timestamp.from(expiresAt)
        );
    }

    @Override
    public String get(String conversationId, String key) {
        List<String> values = jdbcTemplate.query("""
                        SELECT state_value
                        FROM conversation_state
                        WHERE conversation_id = ?
                          AND state_key = ?
                          AND expires_at > ?
                        """,
                (rs, i) -> rs.getString(1),
                conversationId,
                key,
                Timestamp.from(clock.instant())
        );

        return values.isEmpty() ? null : values.getFirst();
    }

    @Override
    public void clear(String conversationId, String key) {
        jdbcTemplate.update("""
                DELETE FROM conversation_state
                WHERE conversation_id = ?
                  AND state_key = ?
                """, conversationId, key);
    }
}
