package com.agenticcare.web.consumer;

import com.agenticcare.dao.entity.AgenticOutreachActionEntity;
import com.agenticcare.dao.repository.AgenticOutreachActionRepository;
import com.agenticcare.domain.admin.service.WorkflowRunService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

/**
 * Polls AWS SQS queues for events from Step Functions/Lambda.
 * Active only when aws-integration profile is enabled.
 * Replaces WorkflowRunEventConsumer (which polls HTTP message broker).
 */
@Component
@Profile("aws-integration")
public class AwsSqsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AwsSqsConsumer.class);
    private final WorkflowRunService runService;
    private final AgenticOutreachActionRepository actionRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SqsClient sqs;

    @Value("${smartcare.aws.sqs.workflow-events-queue-url:}")
    private String workflowEventsQueueUrl;

    @Value("${smartcare.aws.sqs.outreach-results-queue-url:}")
    private String outreachResultsQueueUrl;

    @Value("${smartcare.aws.sqs.admin-alerts-queue-url:}")
    private String adminAlertsQueueUrl;

    public AwsSqsConsumer(WorkflowRunService runService,
                          AgenticOutreachActionRepository actionRepo,
                          @Value("${smartcare.aws.region:us-east-1}") String region) {
        this.runService = runService;
        this.actionRepo = actionRepo;
        this.sqs = SqsClient.builder().region(Region.of(region)).build();
        log.info("AWS SQS consumer initialized for region: {}", region);
    }

    @Scheduled(fixedDelay = 5000)
    public void pollWorkflowEvents() {
        if (workflowEventsQueueUrl == null || workflowEventsQueueUrl.isBlank()) return;
        poll(workflowEventsQueueUrl, this::processWorkflowEvent);
    }

    @Scheduled(fixedDelay = 5000)
    public void pollOutreachResults() {
        if (outreachResultsQueueUrl == null || outreachResultsQueueUrl.isBlank()) return;
        poll(outreachResultsQueueUrl, this::processOutreachResult);
    }

    @Scheduled(fixedDelay = 5000)
    public void pollAdminAlerts() {
        if (adminAlertsQueueUrl == null || adminAlertsQueueUrl.isBlank()) return;
        poll(adminAlertsQueueUrl, this::processAdminAlert);
    }

    private void poll(String queueUrl, java.util.function.Consumer<Message> processor) {
        try {
            ReceiveMessageResponse response = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(1)
                    .build());

            for (Message msg : response.messages()) {
                try {
                    processor.accept(msg);
                    sqs.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(msg.receiptHandle())
                            .build());
                } catch (Exception e) {
                    log.error("Error processing SQS message: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            // Queue might not exist yet
        }
    }

    private void processWorkflowEvent(Message msg) {
        try {
            JsonNode body = mapper.readTree(msg.body());
            JsonNode detail = body.has("detail") ? body.get("detail") : body;

            Long runId = detail.has("runId") ? detail.get("runId").asLong() : null;
            String status = detail.has("status") ? detail.get("status").asText() : null;

            if (runId != null && status != null) {
                String mapped = switch (status.toUpperCase()) {
                    case "RUNNING" -> "RUNNING";
                    case "COMPLETED", "SUCCESS" -> "COMPLETED";
                    case "FAILED" -> "FAILED";
                    default -> null;
                };
                if (mapped != null) {
                    runService.updateStatus(runId, mapped, null, null, null);
                    log.info("SQS: Updated workflow run {} to {}", runId, mapped);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process workflow event: {}", e.getMessage());
        }
    }

    private void processOutreachResult(Message msg) {
        try {
            JsonNode body = mapper.readTree(msg.body());
            JsonNode detail = body.has("detail") ? body.get("detail") : body;
            JsonNode coa = detail.has("coa_result") ? detail.get("coa_result") : detail;

            String patientId = coa.has("patient_id") ? coa.get("patient_id").asText() : "unknown";
            log.info("SQS: Outreach result for patient {}", patientId);

            AgenticOutreachActionEntity action = new AgenticOutreachActionEntity();
            action.setActionKey("aws_" + patientId + "_" + System.currentTimeMillis());
            action.setPatientId(patientId);
            action.setContextState(coa.has("pca_context") ? coa.get("pca_context").asText() : null);
            action.setChannelSelected(coa.has("channel") ? coa.get("channel").asText() : null);
            action.setPatientResponse(coa.has("patient_response") ? coa.get("patient_response").asText() : null);
            action.setActionStatus(coa.has("action_status") ? coa.get("action_status").asText() : "ACTION_TAKEN");
            action.setActionDetailJson(mapper.writeValueAsString(coa));
            action.setWorkflowEngineType("AWS_STEP_FUNCTIONS");
            action.setCreatedBy("aws-sqs-consumer");
            actionRepo.save(action);
        } catch (Exception e) {
            log.error("Failed to process outreach result: {}", e.getMessage());
        }
    }

    private void processAdminAlert(Message msg) {
        try {
            JsonNode body = mapper.readTree(msg.body());
            log.info("SQS: Admin alert received: {}", body.has("detail") ? body.get("detail") : body);
            // TODO: save to admin alerts table / push to dashboard
        } catch (Exception e) {
            log.error("Failed to process admin alert: {}", e.getMessage());
        }
    }
}
