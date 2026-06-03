package ru.meowmure.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProfileStatsListener {

    private static final Logger log = LoggerFactory.getLogger(ProfileStatsListener.class);
    private static final String PROFILE_STATS_PREFIX = "profile:";

    @Autowired
    private ProfileService profileService;

    /**
     * Топик game-results, сообщения с префиксом profile:
     * Формат: profile:playersCsv|winnersCsv|winAmountPerWinner
     */
    @KafkaListener(topics = "game-results", groupId = "auth-profile-group")
    public void handleProfileStats(String message) {
        if (message == null || !message.startsWith(PROFILE_STATS_PREFIX)) {
            return;
        }
        String body = message.substring(PROFILE_STATS_PREFIX.length());
        String[] parts = body.split("\\|", 3);
        if (parts.length != 3) {
            log.warn("[PROFILE STATS] invalid message: {}", message);
            return;
        }
        try {
            long winAmount = Long.parseLong(parts[2].trim());
            profileService.recordGameResult(parts[0], parts[1], winAmount);
            log.info("[PROFILE STATS] applied players={} winners={} winPerWinner={}",
                    parts[0], parts[1], winAmount);
        } catch (NumberFormatException e) {
            log.warn("[PROFILE STATS] invalid win amount in message: {}", message);
        }
    }
}
