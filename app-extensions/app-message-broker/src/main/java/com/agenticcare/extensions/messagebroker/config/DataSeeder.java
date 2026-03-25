package com.agenticcare.extensions.messagebroker.config;

import com.agenticcare.extensions.messagebroker.entity.MessageQueueEntity;
import com.agenticcare.extensions.messagebroker.repository.MessageQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final MessageQueueRepository repo;

    public DataSeeder(MessageQueueRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            log.info("Messages already exist, skipping seed");
            return;
        }

        log.info("Seeding user_profile_event messages...");

        seed("user_profile_event", 1, "profile.created",
                "{\"userId\":\"P-1001\",\"name\":\"Maria Santos\",\"dob\":\"1985-04-12\",\"phone\":\"+1-555-0101\",\"preferredLanguage\":\"en\"}",
                "{\"source\":\"patient-portal\",\"action\":\"REGISTER\"}", "COMPLETED");

        seed("user_profile_event", 2, "profile.created",
                "{\"userId\":\"P-1002\",\"name\":\"James Chen\",\"dob\":\"1972-11-03\",\"phone\":\"+1-555-0102\",\"preferredLanguage\":\"zh\"}",
                "{\"source\":\"patient-portal\",\"action\":\"REGISTER\"}", "COMPLETED");

        seed("user_profile_event", 3, "profile.updated",
                "{\"userId\":\"P-1001\",\"field\":\"phone\",\"oldValue\":\"+1-555-0101\",\"newValue\":\"+1-555-0199\"}",
                "{\"source\":\"patient-portal\",\"action\":\"UPDATE\"}", "COMPLETED");

        seed("user_profile_event", 4, "profile.created",
                "{\"userId\":\"P-1003\",\"name\":\"Aisha Patel\",\"dob\":\"1990-07-22\",\"phone\":\"+1-555-0103\",\"preferredLanguage\":\"hi\"}",
                "{\"source\":\"admin-portal\",\"action\":\"REGISTER\"}", "COMPLETED");

        seed("user_profile_event", 5, "profile.preference_changed",
                "{\"userId\":\"P-1002\",\"field\":\"preferredLanguage\",\"oldValue\":\"zh\",\"newValue\":\"en\"}",
                "{\"source\":\"patient-portal\",\"action\":\"UPDATE\"}", "PENDING");

        seed("user_profile_event", 6, "profile.created",
                "{\"userId\":\"P-1004\",\"name\":\"Robert Williams\",\"dob\":\"1968-01-15\",\"phone\":\"+1-555-0104\",\"preferredLanguage\":\"en\"}",
                "{\"source\":\"patient-portal\",\"action\":\"REGISTER\"}", "PENDING");

        seed("user_profile_event", 7, "profile.deactivated",
                "{\"userId\":\"P-1003\",\"reason\":\"duplicate_account\",\"mergedInto\":\"P-1001\"}",
                "{\"source\":\"admin-portal\",\"action\":\"DEACTIVATE\"}", "FAILED");

        log.info("Seeded 7 user_profile_event messages");
    }

    private void seed(String queue, long seq, String key, String payload, String ctxJson, String status) {
        MessageQueueEntity e = new MessageQueueEntity();
        e.setQueueName(queue);
        e.setSequenceNumber(seq);
        e.setMessageKey(key);
        e.setPayload(payload);
        e.setCtxData(ctxJson);
        e.setStatus(status);
        if ("COMPLETED".equals(status)) {
            e.setProcessedAt(LocalDateTime.now().minusMinutes(seq));
        }
        repo.save(e);
    }
}
