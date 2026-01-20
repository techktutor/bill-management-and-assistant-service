package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.ConversationContext;
import com.wells.bill.assistant.model.ConversationState;
import com.wells.bill.assistant.model.PendingPayment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

@Repository("jdbcConversationStateStore")
public class JdbcConversationStateStore implements ConversationStateStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcConversationStateStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ConversationContext load(
            String conversationId,
            String userId
    ) {
        return jdbcTemplate.query(
                """
                        SELECT conversation_id, user_id, state, bill_id, amount
                        FROM conversation_state
                        WHERE conversation_id = ?
                          AND expires_at > now()
                        """,
                rs -> {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                    return ConversationContext.newConversation(
                            conversationId,
                            userId
                    );
                },
                conversationId
        );
    }

    @Override
    public void save(
            ConversationContext context,
            Duration ttl
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO conversation_state
                        (conversation_id, user_id, state, bill_id, amount, expires_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (conversation_id)
                        DO UPDATE SET
                            state = EXCLUDED.state,
                            bill_id = EXCLUDED.bill_id,
                            amount = EXCLUDED.amount,
                            expires_at = EXCLUDED.expires_at
                        """,
                context.conversationId(),
                context.userId(),
                context.state().name(),
                context.pendingPayment() != null
                        ? context.pendingPayment().billId()
                        : null,
                context.pendingPayment() != null
                        ? context.pendingPayment().amount()
                        : null,
                Timestamp.from(Instant.now().plus(ttl))
        );
    }

    @Override
    public void delete(String conversationId) {
        jdbcTemplate.update("DELETE FROM conversation_state WHERE conversation_id = ?",
                conversationId
        );
    }

    private ConversationContext mapRow(ResultSet rs) throws SQLException {

        PendingPayment pending = null;

        if (rs.getString("bill_id") != null) {
            pending = new PendingPayment(
                    rs.getString("bill_id"),
                    rs.getBigDecimal("amount"),
                    null
            );
        }

        return new ConversationContext(
                rs.getString("conversation_id"),
                rs.getString("user_id"),
                ConversationState.valueOf(rs.getString("state")),
                pending, null
        );
    }
}
