package com.agenticcare.wfs.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AwsStepFunctionsEngineFacade implements WfEngineFacade {

    private static final Logger log = LoggerFactory.getLogger(AwsStepFunctionsEngineFacade.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${smartcare.aws.step-functions.state-machine-arn:}")
    private String stateMachineArn;

    @Override
    public String getEngineType() { return "AWS_STEP_FUNCTIONS"; }

    @Override
    public String triggerRun(String engineBaseUrl, String workflowRef, Long runId, Map<String, Object> parameters) {
        String arn = (stateMachineArn != null && !stateMachineArn.isBlank())
                ? stateMachineArn : engineBaseUrl;

        if (arn == null || arn.isBlank() || !arn.startsWith("arn:")) {
            log.warn("Step Functions ARN not configured. Skipping trigger for runId={}", runId);
            throw new UnsupportedOperationException(
                    "AWS Step Functions not configured. Set smartcare.aws.step-functions.state-machine-arn or start with aws-integration profile.");
        }

        try {
            String input = objectMapper.writeValueAsString(Map.of(
                    "runId", runId,
                    "patient", parameters,
                    "signals", java.util.Collections.emptyList()
            ));

            // Use AWS SDK to start execution
            software.amazon.awssdk.services.sfn.SfnClient sfn = software.amazon.awssdk.services.sfn.SfnClient.create();
            software.amazon.awssdk.services.sfn.model.StartExecutionResponse response = sfn.startExecution(
                    software.amazon.awssdk.services.sfn.model.StartExecutionRequest.builder()
                            .stateMachineArn(arn)
                            .name("run-" + runId + "-" + System.currentTimeMillis())
                            .input(input)
                            .build()
            );

            String executionArn = response.executionArn();
            log.info("Step Functions execution started: {}", executionArn);
            return executionArn;

        } catch (software.amazon.awssdk.services.sfn.model.SfnException e) {
            log.error("Step Functions trigger failed: {}", e.getMessage());
            throw new RuntimeException("Step Functions trigger failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Step Functions trigger failed: {}", e.getMessage());
            throw new RuntimeException("Step Functions trigger failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String checkStatus(String engineBaseUrl, String workflowRef, String externalRunId) {
        if (externalRunId == null || !externalRunId.startsWith("arn:")) return "RUNNING";

        try {
            software.amazon.awssdk.services.sfn.SfnClient sfn = software.amazon.awssdk.services.sfn.SfnClient.create();
            software.amazon.awssdk.services.sfn.model.DescribeExecutionResponse response = sfn.describeExecution(
                    software.amazon.awssdk.services.sfn.model.DescribeExecutionRequest.builder()
                            .executionArn(externalRunId)
                            .build()
            );
            return switch (response.statusAsString()) {
                case "SUCCEEDED" -> "COMPLETED";
                case "FAILED", "TIMED_OUT", "ABORTED" -> "FAILED";
                default -> "RUNNING";
            };
        } catch (Exception e) {
            return "RUNNING";
        }
    }
}
