package com.agenticcare.web.consumer;

import com.agenticcare.domain.admin.service.WorkflowRunService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Consumes workflow_run_event messages from the message broker
 * and updates workflow run status in the database.
 *
 * Polls the broker every 5 seconds for pending messages.
 * This decouples Airflow (or any engine) from Spring Boot —
 * the engine publishes events, this consumer processes them.
 */
@Component
public class WorkflowRunEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRunEventConsumer.class);
    private static final String BROKER_URL = "http://localhost:8081/smart-care/api/broker/v1/messages";
    private static final String QUEUE_NAME = "workflow_run_event";

    private final WorkflowRunService runService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkflowRunEventConsumer(WorkflowRunService runService) {
        this.runService = runService;
    }

    @Scheduled(fixedDelay = 5000)
    public void consumeEvents() {
        try {
            List<Map> messages = restTemplate.getForObject(
                    BROKER_URL + "/consume/" + QUEUE_NAME + "?max=20", List.class);

            if (messages == null || messages.isEmpty()) return;

            for (Map msg : messages) {
                processMessage(msg);
            }
        } catch (Exception e) {
            // Broker might be down — silently skip
        }
    }

    private void processMessage(Map msg) {
        try {
            Long messageId = ((Number) msg.get("id")).longValue();
            String payload = (String) msg.get("payload");

            JsonNode json = objectMapper.readTree(payload);
            Long runId = json.has("runId") ? json.get("runId").asLong() : null;
            String status = json.has("status") ? json.get("status").asText() : null;

            if (runId == null || status == null) {
                log.warn("Skipping workflow_run_event with missing runId or status: {}", payload);
                acknowledge(messageId);
                return;
            }

            // Map DAG status to our status
            String mappedStatus = switch (status.toUpperCase()) {
                case "RUNNING" -> "RUNNING";
                case "COMPLETED", "SUCCESS" -> "COMPLETED";
                case "FAILED", "ERROR" -> "FAILED";
                default -> null;
            };

            if (mappedStatus != null) {
                runService.updateStatus(runId, mappedStatus, null, null, null);
                log.info("Updated workflow run id={} to status={} from broker event", runId, mappedStatus);
            }

            acknowledge(messageId);
        } catch (Exception e) {
            log.error("Error processing workflow_run_event: {}", e.getMessage());
        }
    }

    private void acknowledge(Long messageId) {
        try {
            restTemplate.postForObject(BROKER_URL + "/" + messageId + "/acknowledge", null, Map.class);
        } catch (Exception e) {
            log.warn("Failed to acknowledge message {}: {}", messageId, e.getMessage());
        }
    }
}
