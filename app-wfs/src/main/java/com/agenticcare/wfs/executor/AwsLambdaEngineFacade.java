package com.agenticcare.wfs.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AwsLambdaEngineFacade implements WfEngineFacade {

    private static final Logger log = LoggerFactory.getLogger(AwsLambdaEngineFacade.class);

    @Override
    public String getEngineType() { return "AWS_LAMBDA"; }

    @Override
    public String triggerRun(String engineBaseUrl, String workflowRef, Long runId, Map<String, Object> parameters) {
        log.info("AWS Lambda facade: invoke function={} runId={}", workflowRef, runId);
        // TODO: AWS SDK Lambda InvokeAsync
        throw new UnsupportedOperationException("AWS Lambda facade not yet implemented");
    }

    @Override
    public String checkStatus(String engineBaseUrl, String workflowRef, String externalRunId) {
        return "RUNNING";
    }
}
