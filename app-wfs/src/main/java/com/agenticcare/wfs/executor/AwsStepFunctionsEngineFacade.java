package com.agenticcare.wfs.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class AwsStepFunctionsEngineFacade implements WfEngineFacade {
    private static final Logger log = LoggerFactory.getLogger(AwsStepFunctionsEngineFacade.class);
    @Override public String getEngineType() { return "AWS_STEP_FUNCTIONS"; }
    @Override public String triggerRun(String engineBaseUrl, String workflowRef, Long runId, Map<String, Object> parameters) {
        log.info("Step Functions facade: start execution={} runId={}", workflowRef, runId);
        throw new UnsupportedOperationException("AWS Step Functions facade not yet implemented");
    }
    @Override public String checkStatus(String engineBaseUrl, String workflowRef, String externalRunId) { return "RUNNING"; }
}
