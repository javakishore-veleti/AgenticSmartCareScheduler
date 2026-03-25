package com.agenticcare.web.controller.admin;

import com.agenticcare.common.dto.admin.SecuritySettingRespDto;
import com.agenticcare.dao.entity.SecuritySettingsEntity;
import com.agenticcare.domain.admin.service.SecuritySettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/smart-care/api/admin/v1/security-settings")
@Tag(name = "Admin - Security Settings", description = "Manage secrets and cloud credentials")
public class AdminSecuritySettingsController {

    private static final Logger log = LoggerFactory.getLogger(AdminSecuritySettingsController.class);
    private final SecuritySettingsService service;

    public AdminSecuritySettingsController(SecuritySettingsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all security settings (safe — no secrets exposed)")
    public ResponseEntity<List<SecuritySettingRespDto>> getAll() {
        log.info(">>> GET /security-settings called");
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/type/{settingType}")
    @Operation(summary = "List security settings by type (safe)")
    public ResponseEntity<List<SecuritySettingRespDto>> getByType(@PathVariable String settingType) {
        return ResponseEntity.ok(service.getByType(settingType));
    }

    @PostMapping
    @Operation(summary = "Create a new security setting")
    public ResponseEntity<SecuritySettingRespDto> create(@RequestBody SecuritySettingsEntity entity) {
        log.info(">>> POST /security-settings name={} type={}", entity.getSettingName(), entity.getSettingType());
        return ResponseEntity.ok(service.create(entity));
    }

    @GetMapping("/{id}/edit-info")
    @Operation(summary = "Get safe fields for editing (no secrets)")
    public ResponseEntity<SecuritySettingRespDto> getEditInfo(@PathVariable Long id) {
        log.info(">>> GET /security-settings/{}/edit-info called", id);
        return ResponseEntity.ok(service.getEditInfo(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a security setting")
    public ResponseEntity<SecuritySettingRespDto> update(@PathVariable Long id, @RequestBody SecuritySettingsEntity entity) {
        log.info(">>> PUT /security-settings/{} name={}", id, entity.getSettingName());
        return ResponseEntity.ok(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a security setting")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
