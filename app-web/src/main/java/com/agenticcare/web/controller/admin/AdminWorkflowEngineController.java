package com.agenticcare.web.controller.admin;

import com.agenticcare.dao.entity.WorkflowEngineMasterEntity;
import com.agenticcare.domain.admin.service.WorkflowEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/smart-care/api/admin/v1/workflow-engines")
@Tag(name = "Admin - Workflow Engines", description = "Manage workflow engine definitions (Airflow, EMR, Databricks)")
public class AdminWorkflowEngineController {

    private static final Logger log = LoggerFactory.getLogger(AdminWorkflowEngineController.class);
    private final WorkflowEngineService service;

    public AdminWorkflowEngineController(WorkflowEngineService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all workflow engines")
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        log.info(">>> GET /workflow-engines called");
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow engine by ID")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        log.info(">>> GET /workflow-engines/{} called", id);
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create workflow engine")
    public ResponseEntity<Map<String, Object>> create(@RequestBody WorkflowEngineMasterEntity entity) {
        log.info(">>> POST /workflow-engines name={} type={}", entity.getEngineName(), entity.getEngineType());
        return ResponseEntity.ok(service.create(entity));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update workflow engine")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @RequestBody WorkflowEngineMasterEntity entity) {
        log.info(">>> PUT /workflow-engines/{} name={}", id, entity.getEngineName());
        return ResponseEntity.ok(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete workflow engine")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        log.info(">>> DELETE /workflow-engines/{}", id);
        service.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
