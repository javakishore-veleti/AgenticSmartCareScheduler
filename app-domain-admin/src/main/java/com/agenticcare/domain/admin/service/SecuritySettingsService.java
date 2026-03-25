package com.agenticcare.domain.admin.service;

import com.agenticcare.common.dto.admin.SecuritySettingRespDto;
import com.agenticcare.dao.entity.SecuritySettingsEntity;
import com.agenticcare.dao.repository.SecuritySettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SecuritySettingsService {

    private static final Logger log = LoggerFactory.getLogger(SecuritySettingsService.class);
    private final SecuritySettingsRepository repo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecuritySettingsService(SecuritySettingsRepository repo) {
        this.repo = repo;
    }

    public List<SecuritySettingRespDto> getAll() {
        return repo.findAll().stream().map(this::toSafeDto).collect(Collectors.toList());
    }

    public List<SecuritySettingRespDto> getByType(String settingType) {
        return repo.findBySettingType(settingType).stream().map(this::toSafeDto).collect(Collectors.toList());
    }

    public SecuritySettingRespDto create(SecuritySettingsEntity entity) {
        log.info("Creating security setting: {} type={}", entity.getSettingName(), entity.getSettingType());
        return toSafeDto(repo.save(entity));
    }

    public SecuritySettingRespDto getEditInfo(Long id) {
        SecuritySettingsEntity entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + id));
        SecuritySettingRespDto dto = toSafeDto(entity);
        // For edit, include non-secret config fields
        try {
            JsonNode config = objectMapper.readTree(entity.getConfigsJson());
            if ("AWS_CLIENT_PROFILE".equals(entity.getSettingType())) {
                dto.setSummary(config.has("profileName") ? config.get("profileName").asText() : "");
            } else if ("AWS_CLIENT_CREDENTIALS".equals(entity.getSettingType())) {
                // Only return region, never keys
                dto.setSummary(config.has("region") ? config.get("region").asText() : "");
            }
        } catch (Exception ignored) {}
        return dto;
    }

    public SecuritySettingRespDto update(Long id, SecuritySettingsEntity updated) {
        SecuritySettingsEntity entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + id));
        entity.setSettingName(updated.getSettingName());
        if (updated.getConfigsJson() != null && !updated.getConfigsJson().isBlank()) {
            entity.setConfigsJson(updated.getConfigsJson());
        }
        log.info("Updated security setting id={} name={}", id, entity.getSettingName());
        return toSafeDto(repo.save(entity));
    }

    public void delete(Long id) {
        log.info("Deleting security setting id={}", id);
        repo.deleteById(id);
    }

    private SecuritySettingRespDto toSafeDto(SecuritySettingsEntity entity) {
        SecuritySettingRespDto dto = new SecuritySettingRespDto();
        dto.setId(entity.getId());
        dto.setSettingName(entity.getSettingName());
        dto.setSettingType(entity.getSettingType());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        dto.setSummary(buildSafeSummary(entity));
        return dto;
    }

    private String buildSafeSummary(SecuritySettingsEntity entity) {
        try {
            JsonNode config = objectMapper.readTree(entity.getConfigsJson());
            String type = entity.getSettingType();

            if ("AWS_CLIENT_PROFILE".equals(type)) {
                String profile = config.has("profileName") ? config.get("profileName").asText() : "unknown";
                String region = config.has("region") ? config.get("region").asText() : "";
                return "Profile: " + profile + (region.isEmpty() ? "" : " (" + region + ")");
            } else if ("AWS_CLIENT_CREDENTIALS".equals(type)) {
                String keyId = config.has("accessKeyId") ? config.get("accessKeyId").asText() : "";
                String masked = keyId.length() > 4 ? keyId.substring(0, 4) + "****" : "****";
                String region = config.has("region") ? config.get("region").asText() : "";
                return "Key: " + masked + (region.isEmpty() ? "" : " (" + region + ")");
            } else {
                return entity.getSettingType();
            }
        } catch (Exception e) {
            return entity.getSettingType();
        }
    }
}
