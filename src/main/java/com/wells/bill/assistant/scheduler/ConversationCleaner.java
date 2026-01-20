package com.wells.bill.assistant.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationCleaner {

    private final JdbcTemplate jdbcTemplate;

    // every 10 minutes
    @Scheduled(cron = "0 */10 * * * *")
    public void cleanupExpiredStates() {
        int deleted = jdbcTemplate.update(
                "DELETE FROM conversation_state WHERE expires_at <= now()"
        );
        if (deleted > 0) {
            log.info("Cleaned up {} expired conversation states", deleted);
        }
    }
}
