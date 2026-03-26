package com.agenticcare.wfs.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class AwsEmrEngineFacade implements WfEngineFacade {
    private static final Logger log = LoggerFactory.getLogger(AwsEmrEngineFacade.class);
    @Override public String getEngineType() { return "AWS_EMR"; }
    @Override public String triggerRun(String engineBaseUrl, String workflowRef, Long runId, Map<String, Object> parameters) {
        log.info("AWS EMR facade: trigger step={} runId={}", workflowRef, runId);
        throw new UnsupportedOperationException("AWS EMR facade not yet implemented");
    }
    @Override public String checkStatus(String engineBaseUrl, String workflowRef, String externalRunId) { return "RUNNING"; }
}
