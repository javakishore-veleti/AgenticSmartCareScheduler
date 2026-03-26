package com.agenticcare.domain.admin.service;

import com.agenticcare.dao.entity.WorkflowDefinitionMasterEntity;
import com.agenticcare.dao.entity.WorkflowEngineMappingEntity;
import com.agenticcare.dao.entity.WorkflowEngineMasterEntity;
import com.agenticcare.dao.repository.WorkflowDefinitionMasterRepository;
import com.agenticcare.dao.repository.WorkflowEngineMappingRepository;
import com.agenticcare.dao.repository.WorkflowEngineMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkflowDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDefinitionService.class);
    private final WorkflowDefinitionMasterRepository repo;
    private final WorkflowEngineMappingRepository mappingRepo;
    private final WorkflowEngineMasterRepository engineRepo;

    public WorkflowDefinitionService(WorkflowDefinitionMasterRepository repo,
                                     WorkflowEngineMappingRepository mappingRepo,
                                     WorkflowEngineMasterRepository engineRepo) {
        this.repo = repo;
        this.mappingRepo = mappingRepo;
        this.engineRepo = engineRepo;
    }

    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        return toDto(repo.findById(id).orElseThrow(() -> new RuntimeException("Definition not found: " + id)));
    }

    public void seedDefaults() {
        seedOne("cp_simulation", "Context State Simulation",
                "Assigns context states (C_p) to patient records based on appointment time, SMS receipt, and risk score. Produces channel distribution stats.");
        seedOne("model_retrain", "XGBoost Model Retrain",
                "Retrains the PCA risk model (XGBoost) on the Medical Appointment No-Show dataset. Outputs F1, AUC, confusion matrix, and saved model.");
        seedOne("data_quality_check", "Data Quality Check",
                "Validates dataset integrity: null checks, range validation, duplicate detection, schema conformance. Produces quality report.");
        log.info("Seeded default workflow definitions");
    }

    private void seedOne(String key, String displayName, String description) {
        if (repo.findByWorkflowKey(key).isPresent()) {
            log.info("Workflow definition already exists: {}", key);
            return;
        }
        WorkflowDefinitionMasterEntity entity = new WorkflowDefinitionMasterEntity();
        entity.setWorkflowKey(key);
        entity.setDisplayName(displayName);
        entity.setDescription(description);
        entity.setStatus("ACTIVE");
        repo.save(entity);
        log.info("Seeded workflow definition: {}", key);
    }

    public Map<String, Object> create(Map<String, Object> req) {
        WorkflowDefinitionMasterEntity entity = new WorkflowDefinitionMasterEntity();
        entity.setWorkflowKey((String) req.get("workflowKey"));
        entity.setDisplayName((String) req.get("displayName"));
        entity.setDescription((String) req.get("description"));
        entity.setParametersSchema((String) req.get("parametersSchema"));
        entity.setStatus("ACTIVE");
        log.info("Creating workflow definition: {}", entity.getWorkflowKey());
        return toDto(repo.save(entity));
    }

    public Map<String, Object> update(Long id, Map<String, Object> req) {
        WorkflowDefinitionMasterEntity entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Definition not found: " + id));
        if (req.containsKey("displayName")) entity.setDisplayName((String) req.get("displayName"));
        if (req.containsKey("description")) entity.setDescription((String) req.get("description"));
        if (req.containsKey("parametersSchema")) entity.setParametersSchema((String) req.get("parametersSchema"));
        if (req.containsKey("status")) entity.setStatus((String) req.get("status"));
        log.info("Updated workflow definition id={} key={}", id, entity.getWorkflowKey());
        return toDto(repo.save(entity));
    }

    public void delete(Long id) {
        log.info("Deleting workflow definition id={}", id);
        repo.deleteById(id);
    }

    // --- Engine Mapping ---

    public List<Map<String, Object>> getEnginesForDefinition(Long definitionId) {
        return mappingRepo.findByWorkflowDefinitionId(definitionId).stream()
                .map(this::mappingToDto).collect(Collectors.toList());
    }

    public Map<String, Object> addEngineMapping(Long definitionId, Map<String, Object> req) {
        WorkflowDefinitionMasterEntity def = repo.findById(definitionId)
                .orElseThrow(() -> new RuntimeException("Definition not found: " + definitionId));
        Long engineId = ((Number) req.get("engineId")).longValue();
        WorkflowEngineMasterEntity engine = engineRepo.findById(engineId)
                .orElseThrow(() -> new RuntimeException("Engine not found: " + engineId));

        WorkflowEngineMappingEntity mapping = new WorkflowEngineMappingEntity();
        mapping.setWorkflowDefinition(def);
        mapping.setEngine(engine);
        mapping.setEngineWorkflowRef((String) req.get("engineWorkflowRef"));
        mapping.setConfigsJson((String) req.get("configsJson"));
        mapping.setStatus("ACTIVE");

        log.info("Mapping definition={} to engine={} ref={}", def.getWorkflowKey(), engine.getEngineName(), mapping.getEngineWorkflowRef());
        return mappingToDto(mappingRepo.save(mapping));
    }

    public void removeEngineMapping(Long mappingId) {
        log.info("Removing engine mapping id={}", mappingId);
        mappingRepo.deleteById(mappingId);
    }

    private Map<String, Object> toDto(WorkflowDefinitionMasterEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("workflowKey", e.getWorkflowKey());
        m.put("displayName", e.getDisplayName());
        m.put("description", e.getDescription());
        m.put("parametersSchema", e.getParametersSchema());
        m.put("status", e.getStatus());
        m.put("createdAt", e.getCreatedAt());
        m.put("updatedAt", e.getUpdatedAt());
        List<Map<String, Object>> engines = mappingRepo.findByWorkflowDefinitionIdAndStatus(e.getId(), "ACTIVE")
                .stream().map(this::mappingToDto).collect(Collectors.toList());
        m.put("engines", engines);
        return m;
    }

    private Map<String, Object> mappingToDto(WorkflowEngineMappingEntity m) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("mappingId", m.getId());
        dto.put("engineId", m.getEngine().getId());
        dto.put("engineName", m.getEngine().getEngineName());
        dto.put("engineType", m.getEngine().getEngineType());
        dto.put("engineWorkflowRef", m.getEngineWorkflowRef());
        dto.put("status", m.getStatus());
        return dto;
    }
}
