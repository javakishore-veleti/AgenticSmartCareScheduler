package com.agenticcare.wfs.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class DatabricksEngineFacade implements WfEngineFacade {
    private static final Logger log = LoggerFactory.getLogger(DatabricksEngineFacade.class);
    @Override public String getEngineType() { return "DATABRICKS"; }
    @Override public String triggerRun(String engineBaseUrl, String workflowRef, Long runId, Map<String, Object> parameters) {
        log.info("Databricks facade: trigger job={} runId={}", workflowRef, runId);
        throw new UnsupportedOperationException("Databricks facade not yet implemented");
    }
    @Override public String checkStatus(String engineBaseUrl, String workflowRef, String externalRunId) { return "RUNNING"; }
}
