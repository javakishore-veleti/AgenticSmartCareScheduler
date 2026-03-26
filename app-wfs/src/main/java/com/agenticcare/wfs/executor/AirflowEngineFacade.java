package com.agenticcare.wfs.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AirflowEngineFacade implements WfEngineFacade {

    private static final Logger log = LoggerFactory.getLogger(AirflowEngineFacade.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getEngineType() { return "AIRFLOW"; }

    @Override
    public String triggerRun(String engineBaseUrl, String workflowRef, Long runId, Map<String, Object> parameters) {
        String url = engineBaseUrl + "/api/v1/dags/" + workflowRef + "/dagRuns";

        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> conf = new LinkedHashMap<>();
        conf.put("runId", runId);
        if (parameters != null) conf.putAll(parameters);
        body.put("conf", conf);

        try {
            log.info("Triggering Airflow DAG: {} at {} runId={}", workflowRef, url, runId);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, createHeaders()), Map.class);

            String dagRunId = response.getBody() != null ? (String) response.getBody().get("dag_run_id") : "unknown";
            log.info("Airflow DAG triggered: dagRunId={}", dagRunId);
            return dagRunId;
        } catch (Exception e) {
            log.error("Airflow trigger failed for DAG {}: {}", workflowRef, e.getMessage());
            throw new RuntimeException("Airflow trigger failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String checkStatus(String engineBaseUrl, String workflowRef, String externalRunId) {
        String url = engineBaseUrl + "/api/v1/dags/" + workflowRef + "/dagRuns/" + externalRunId;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(createHeaders()), Map.class);
            String state = response.getBody() != null ? (String) response.getBody().get("state") : "running";
            return switch (state) {
                case "success" -> "COMPLETED";
                case "failed" -> "FAILED";
                default -> "RUNNING";
            };
        } catch (Exception e) {
            log.error("Airflow status check failed: {}", e.getMessage());
            return "RUNNING";
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes()));
        return h;
    }
}
