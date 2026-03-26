package com.agenticcare.web.controller.admin;

import com.agenticcare.core.service.DatasetService;
import com.agenticcare.dao.entity.SystemSettingsLogEntity;
import com.agenticcare.domain.admin.service.SystemSetupService;
import com.agenticcare.domain.admin.service.WorkflowDefinitionService;
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
@RequestMapping("/smart-care/api/admin/v1/system-setup")
@Tag(name = "Admin - System Setup", description = "Foundational data setup status and seeding")
public class AdminSystemSetupController {

    private static final Logger log = LoggerFactory.getLogger(AdminSystemSetupController.class);
    private final SystemSetupService setupService;
    private final DatasetService datasetService;
    private final WorkflowDefinitionService defService;
    private final WorkflowEngineService engineService;

    public AdminSystemSetupController(SystemSetupService setupService,
                                      DatasetService datasetService,
                                      WorkflowDefinitionService defService,
                                      WorkflowEngineService engineService) {
        this.setupService = setupService;
        this.datasetService = datasetService;
        this.defService = defService;
        this.engineService = engineService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get foundational data setup status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.info(">>> GET /system-setup/status called");
        return ResponseEntity.ok(setupService.getSetupStatus());
    }

    @GetMapping("/logs")
    @Operation(summary = "Get setup activity log")
    public ResponseEntity<List<SystemSettingsLogEntity>> getLogs() {
        log.info(">>> GET /system-setup/logs called");
        return ResponseEntity.ok(setupService.getAllLogs());
    }

    @PostMapping("/seed-all")
    @Operation(summary = "Seed all foundational data")
    public ResponseEntity<Map<String, String>> seedAll() {
        log.info(">>> POST /system-setup/seed-all called");

        if (!setupService.isSeeded(SystemSetupService.DATASETS_SEEDED)) {
            datasetService.seedDefaultDatasets();
            setupService.recordSeed(SystemSetupService.DATASETS_SEEDED, "MEDICAL_APPT_NOSHOW dataset");
        }
        if (!setupService.isSeeded(SystemSetupService.WORKFLOWS_SEEDED)) {
            defService.seedDefaults();
            setupService.recordSeed(SystemSetupService.WORKFLOWS_SEEDED, "5 agentic outreach workflows");
        }
        if (!setupService.isSeeded(SystemSetupService.ENGINES_SEEDED)) {
            engineService.seedDefaults();
            setupService.recordSeed(SystemSetupService.ENGINES_SEEDED, "local-airflow engine");
        }

        return ResponseEntity.ok(Map.of("status", "all_seeded"));
    }

    @PostMapping("/seed/{key}")
    @Operation(summary = "Seed a specific foundational data category")
    public ResponseEntity<Map<String, String>> seedOne(@PathVariable String key) {
        log.info(">>> POST /system-setup/seed/{} called", key);

        switch (key) {
            case "datasets" -> {
                datasetService.seedDefaultDatasets();
                setupService.recordSeed(SystemSetupService.DATASETS_SEEDED, "MEDICAL_APPT_NOSHOW dataset");
            }
            case "workflows" -> {
                defService.seedDefaults();
                setupService.recordSeed(SystemSetupService.WORKFLOWS_SEEDED, "5 agentic outreach workflows");
            }
            case "engines" -> {
                engineService.seedDefaults();
                setupService.recordSeed(SystemSetupService.ENGINES_SEEDED, "local-airflow engine");
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown key: " + key));
            }
        }

        return ResponseEntity.ok(Map.of("status", "seeded", "key", key));
    }
}
