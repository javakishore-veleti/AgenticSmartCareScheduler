package com.agenticcare.web.controller.admin;

import com.agenticcare.domain.admin.service.WorkflowRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @GetMapping("/{id}/results")
    @Operation(summary = "Get workflow run results JSON")
    public ResponseEntity<?> getResults(@PathVariable Long id) {
        log.info(">>> GET /workflow-runs/{}/results called", id);
        Path outputDir = Path.of(System.getProperty("user.home"),
                "runtime_data", "workflow_output", "run_" + id);
        Path resultsFile = outputDir.resolve("results.json");
        if (!Files.exists(resultsFile)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String json = Files.readString(resultsFile);
            ObjectMapper mapper = new ObjectMapper();
            Object parsed = mapper.readValue(json, Object.class);
            return ResponseEntity.ok(parsed);
        } catch (Exception e) {
            log.error("Failed to read results for run {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/chart/{filename}")
    @Operation(summary = "Serve chart image from workflow run output directory")
    public ResponseEntity<Resource> getChart(@PathVariable Long id, @PathVariable String filename) {
        log.info(">>> GET /workflow-runs/{}/chart/{} called", id, filename);
        // Sanitize filename to prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path outputDir = Path.of(System.getProperty("user.home"),
                "runtime_data", "workflow_output", "run_" + id);
        File chartFile = outputDir.resolve(filename).toFile();
        if (!chartFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(chartFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .body(resource);
    }
}
