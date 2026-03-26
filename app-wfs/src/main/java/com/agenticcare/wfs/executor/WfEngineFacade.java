package com.agenticcare.wfs.executor;

import java.util.Map;

/**
 * Facade interface for workflow engine integrations.
 * Implementations: AirflowEngineFacade, AwsEmrEngineFacade,
 * AwsLambdaEngineFacade, AwsStepFunctionsEngineFacade,
 * DatabricksEngineFacade, RestApiEngineFacade.
 */
public interface WfEngineFacade {

    String getEngineType();

    String triggerRun(String engineBaseUrl, String workflowRef, Long runId, Map<String, Object> parameters);

    String checkStatus(String engineBaseUrl, String workflowRef, String externalRunId);
}
