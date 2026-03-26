package com.agenticcare.wfs.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class RestApiEngineFacade implements WfEngineFacade {

    private static final Logger log = LoggerFactory.getLogger(RestApiEngineFacade.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getEngineType() { return "REST_API"; }

    @Override
    public String triggerRun(String engineBaseUrl, String workflowRef, Long runId, Map<String, Object> parameters) {
        String url = engineBaseUrl + "/" + workflowRef;
        log.info("REST API facade: POST {} runId={}", url, runId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(parameters, headers), Map.class);
            return response.getBody() != null && response.getBody().containsKey("runId")
                    ? String.valueOf(response.getBody().get("runId"))
                    : "rest-" + runId;
        } catch (Exception e) {
            log.error("REST API trigger failed: {}", e.getMessage());
            throw new RuntimeException("REST API trigger failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String checkStatus(String engineBaseUrl, String workflowRef, String externalRunId) {
        return "RUNNING";
    }
}
