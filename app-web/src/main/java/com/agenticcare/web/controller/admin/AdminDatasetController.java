package com.agenticcare.web.controller.admin;

import com.agenticcare.common.dto.admin.DatasetDetailsRespDto;
import com.agenticcare.core.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/smart-care/api/admin/v1/analytics/datasets")
@Tag(name = "Admin - Datasets", description = "Dataset management for analytics and ML")
public class AdminDatasetController {

    private static final Logger log = LoggerFactory.getLogger(AdminDatasetController.class);

    private final DatasetService datasetService;

    public AdminDatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @GetMapping
    @Operation(summary = "List all registered datasets")
    public ResponseEntity<List<DatasetDetailsRespDto>> getAllDatasets() {
        log.info(">>> GET /smart-care/api/admin/v1/analytics/datasets called");
        List<DatasetDetailsRespDto> result = datasetService.getAllDatasets();
        log.info(">>> Returning {} datasets", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{datasetCode}/getDetails")
    @Operation(summary = "Get dataset details including storage instances and status")
    public ResponseEntity<DatasetDetailsRespDto> getDatasetDetails(@PathVariable String datasetCode) {
        log.info(">>> GET /datasets/{}/getDetails called", datasetCode);
        return ResponseEntity.ok(datasetService.getDatasetDetails(datasetCode));
    }

    @PostMapping("/seed-defaults")
    @Operation(summary = "Seed default datasets into the database (on-demand, not at startup)")
    public ResponseEntity<Map<String, String>> seedDefaults() {
        log.info(">>> POST /datasets/seed-defaults called");
        datasetService.seedDefaultDatasets();
        return ResponseEntity.ok(Map.of("status", "seeded", "message", "Default datasets registered successfully"));
    }
}
