package com.wells.bill.assistant.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationStateCleanupJob {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Scheduled(cron = "0 */10 * * * *") // every 10 minutes
    public void cleanupExpiredStates() {
        int deleted = jdbcTemplate.update("""
            DELETE FROM conversation_state
            WHERE expires_at <= ?
            """, Timestamp.from(clock.instant()));
        if (deleted > 0) {
            log.info("Cleaned up {} expired conversation states", deleted);
        }
    }
}
