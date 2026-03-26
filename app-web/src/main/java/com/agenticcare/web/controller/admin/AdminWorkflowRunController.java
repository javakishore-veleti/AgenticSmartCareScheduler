package com.agenticcare.web.controller.admin;

import com.agenticcare.domain.admin.service.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/smart-care/api/admin/v1/workflow-runs")
@Tag(name = "Admin - Workflow Runs", description = "Submit and track workflow executions")
public class AdminWorkflowRunController {

    private static final Logger log = LoggerFactory.getLogger(AdminWorkflowRunController.class);
    private final WorkflowRunService service;

    public AdminWorkflowRunController(WorkflowRunService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all workflow runs")
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        log.info(">>> GET /workflow-runs called");
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow run by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        log.info(">>> GET /workflow-runs/{} called", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/by-definition/{defId}")
    @Operation(summary = "List runs for a workflow definition")
    public ResponseEntity<List<Map<String, Object>>> getByDefinition(@PathVariable Long defId) {
        log.info(">>> GET /workflow-runs/by-definition/{} called", defId);
        return ResponseEntity.ok(service.getByDefinitionId(defId));
    }

    @GetMapping("/by-dataset-instance/{instanceId}")
    @Operation(summary = "List runs for a dataset instance")
    public ResponseEntity<List<Map<String, Object>>> getByDatasetInstance(@PathVariable Long instanceId) {
        log.info(">>> GET /workflow-runs/by-dataset-instance/{} called", instanceId);
        return ResponseEntity.ok(service.getByDatasetInstanceId(instanceId));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit a new workflow run")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody Map<String, Object> req) {
        log.info(">>> POST /workflow-runs/submit defId={}", req.get("workflowDefinitionId"));
        return ResponseEntity.ok(service.submit(req));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update workflow run status (called by broker consumer)")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                             @RequestBody Map<String, String> req) {
        log.info(">>> PUT /workflow-runs/{}/status status={}", id, req.get("status"));
        return ResponseEntity.ok(service.updateStatus(id,
                req.get("status"), req.get("externalRunId"),
                req.get("resultPath"), req.get("errorMessage")));
    }
}
