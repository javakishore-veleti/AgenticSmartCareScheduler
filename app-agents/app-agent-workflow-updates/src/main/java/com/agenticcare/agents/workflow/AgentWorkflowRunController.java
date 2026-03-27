package com.agenticcare.agents.workflow;

import com.agenticcare.domain.admin.service.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal API for workflow engines (Airflow, EMR, etc.) to update
 * workflow run status back to the admin system.
 *
 * NOT exposed to admin UI or customer UI — only agents and workflow engines call this.
 */
@RestController
@RequestMapping("/smart-care/api/agents/admin/v1/workflow-runs")
@Tag(name = "Agents - Workflow Run Updates", description = "Internal API for agents/engines to update workflow run status")
public class AgentWorkflowRunController {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowRunController.class);
    private final WorkflowRunService runService;

    public AgentWorkflowRunController(WorkflowRunService runService) {
        this.runService = runService;
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update workflow run status (called by Airflow DAG, EMR step, etc.)")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                             @RequestBody Map<String, String> req) {
        log.info(">>> PUT /agents/admin/workflow-runs/{}/status status={}", id, req.get("status"));
        return ResponseEntity.ok(runService.updateStatus(id,
                req.get("status"), req.get("externalRunId"),
                req.get("resultPath"), req.get("errorMessage")));
    }
}
