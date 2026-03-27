package com.agenticcare.web.controller.admin;

import com.agenticcare.dao.entity.AgenticOutreachActionEntity;
import com.agenticcare.dao.repository.AgenticOutreachActionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/smart-care/api/admin/v1/agentic-actions")
@Tag(name = "Admin - Agentic Actions", description = "Per-patient outreach action records from agentic AI")
public class AdminAgenticActionController {

    private static final Logger log = LoggerFactory.getLogger(AdminAgenticActionController.class);
    private final AgenticOutreachActionRepository repo;

    public AdminAgenticActionController(AgenticOutreachActionRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    @Operation(summary = "Record an agentic outreach action (called by agent DAG)")
    public ResponseEntity<Map<String, Object>> recordAction(@RequestBody Map<String, Object> req) {
        String actionKey = (String) req.get("actionKey");
        log.info(">>> POST /agentic-actions key={}", actionKey);

        // Upsert — skip if already recorded (idempotent)
        if (actionKey != null && repo.findByActionKey(actionKey).isPresent()) {
            return ResponseEntity.ok(Map.of("status", "already_exists", "actionKey", actionKey));
        }

        AgenticOutreachActionEntity entity = new AgenticOutreachActionEntity();
        entity.setActionKey(actionKey);
        entity.setWorkflowRunId(req.get("workflowRunId") != null ? ((Number) req.get("workflowRunId")).longValue() : null);
        entity.setWorkflowEngineType((String) req.get("workflowEngineType"));
        entity.setEngineInstanceId((String) req.get("engineInstanceId"));
        entity.setPatientId((String) req.get("patientId"));
        entity.setContextState((String) req.get("contextState"));
        entity.setChannelSelected((String) req.get("channelSelected"));
        entity.setPatientResponse((String) req.get("patientResponse"));
        entity.setActionStatus((String) req.get("actionStatus"));
        entity.setActionDetailJson((String) req.get("actionDetailJson"));
        entity.setCreatedBy("agentic-agent");

        entity = repo.save(entity);
        log.info("Recorded agentic action: key={} patient={} status={}", actionKey, entity.getPatientId(), entity.getActionStatus());

        return ResponseEntity.ok(Map.of("status", "recorded", "id", entity.getId(), "actionKey", actionKey));
    }

    @GetMapping("/by-run/{workflowRunId}")
    @Operation(summary = "Get all actions for a workflow run")
    public ResponseEntity<List<AgenticOutreachActionEntity>> getByRun(@PathVariable Long workflowRunId) {
        log.info(">>> GET /agentic-actions/by-run/{}", workflowRunId);
        return ResponseEntity.ok(repo.findByWorkflowRunIdOrderByCreatedAtDesc(workflowRunId));
    }

    @GetMapping("/by-patient/{patientId}")
    @Operation(summary = "Get all actions for a patient")
    public ResponseEntity<List<AgenticOutreachActionEntity>> getByPatient(@PathVariable String patientId) {
        log.info(">>> GET /agentic-actions/by-patient/{}", patientId);
        return ResponseEntity.ok(repo.findByPatientId(patientId));
    }
}
