package com.agenticcare.web.controller.admin;

import com.agenticcare.domain.admin.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/smart-care/api/admin/v1/workflow-definitions")
@Tag(name = "Admin - Workflow Definitions", description = "Manage product workflow definitions and engine mappings")
public class AdminWorkflowDefinitionController {

    private static final Logger log = LoggerFactory.getLogger(AdminWorkflowDefinitionController.class);
    private final WorkflowDefinitionService service;

    public AdminWorkflowDefinitionController(WorkflowDefinitionService service) {
        this.service = service;
    }

    @PostMapping("/seed-defaults")
    @Operation(summary = "Seed default workflow definitions from product DAGs")
    public ResponseEntity<Map<String, String>> seedDefaults() {
        log.info(">>> POST /workflow-definitions/seed-defaults called");
        service.seedDefaults();
        return ResponseEntity.ok(Map.of("status", "seeded"));
    }

    @GetMapping
    @Operation(summary = "List all workflow definitions")
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        log.info(">>> GET /workflow-definitions called");
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow definition by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        log.info(">>> GET /workflow-definitions/{} called", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create workflow definition")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> req) {
        log.info(">>> POST /workflow-definitions key={}", req.get("workflowKey"));
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update workflow definition")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> req) {
        log.info(">>> PUT /workflow-definitions/{}", id);
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete workflow definition")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        log.info(">>> DELETE /workflow-definitions/{}", id);
        service.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    // --- Engine Mappings ---

    @GetMapping("/{id}/engines")
    @Operation(summary = "List compatible engines for a workflow definition")
    public ResponseEntity<List<Map<String, Object>>> getEngines(@PathVariable Long id) {
        log.info(">>> GET /workflow-definitions/{}/engines called", id);
        return ResponseEntity.ok(service.getEnginesForDefinition(id));
    }

    @PostMapping("/{id}/engines")
    @Operation(summary = "Map a workflow definition to an engine")
    public ResponseEntity<Map<String, Object>> addEngine(@PathVariable Long id,
                                                          @RequestBody Map<String, Object> req) {
        log.info(">>> POST /workflow-definitions/{}/engines engineId={}", id, req.get("engineId"));
        return ResponseEntity.ok(service.addEngineMapping(id, req));
    }

    @DeleteMapping("/engine-mappings/{mappingId}")
    @Operation(summary = "Remove engine mapping")
    public ResponseEntity<Map<String, String>> removeEngine(@PathVariable Long mappingId) {
        log.info(">>> DELETE /workflow-definitions/engine-mappings/{}", mappingId);
        service.removeEngineMapping(mappingId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
