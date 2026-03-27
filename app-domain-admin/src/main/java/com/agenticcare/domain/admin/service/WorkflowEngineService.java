package com.agenticcare.domain.admin.service;

import com.agenticcare.dao.entity.WorkflowEngineMasterEntity;
import com.agenticcare.dao.repository.WorkflowEngineMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkflowEngineService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineService.class);
    private final WorkflowEngineMasterRepository repo;

    public WorkflowEngineService(WorkflowEngineMasterRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        return toDto(repo.findById(id).orElseThrow(() -> new RuntimeException("Engine not found: " + id)));
    }

    public Map<String, Object> create(WorkflowEngineMasterEntity entity) {
        log.info("Creating workflow engine: {} type={}", entity.getEngineName(), entity.getEngineType());
        return toDto(repo.save(entity));
    }

    public Map<String, Object> update(Long id, WorkflowEngineMasterEntity req) {
        WorkflowEngineMasterEntity entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Engine not found: " + id));
        entity.setEngineName(req.getEngineName());
        entity.setEngineType(req.getEngineType());
        entity.setBaseUrl(req.getBaseUrl());
        entity.setAuthType(req.getAuthType());
        entity.setConfigsJson(req.getConfigsJson());
        entity.setStatus(req.getStatus());
        entity.setDescription(req.getDescription());
        log.info("Updated workflow engine id={} name={}", id, entity.getEngineName());
        return toDto(repo.save(entity));
    }

    public void seedDefaults() {
        if (repo.findByEngineName("local-airflow").isEmpty()) {
            WorkflowEngineMasterEntity engine = new WorkflowEngineMasterEntity();
            engine.setEngineName("local-airflow");
            engine.setEngineType("AIRFLOW");
            engine.setBaseUrl("http://localhost:8082");
            engine.setAuthType("BASIC");
            engine.setDescription("Local Apache Airflow instance (Docker Compose on port 8082). Login: admin/admin.");
            engine.setStatus("ACTIVE");
            repo.save(engine);
            log.info("Seeded default workflow engine: local-airflow");
        }
        if (repo.findByEngineName("aws-step-functions").isEmpty()) {
            WorkflowEngineMasterEntity engine = new WorkflowEngineMasterEntity();
            engine.setEngineName("aws-step-functions");
            engine.setEngineType("AWS_STEP_FUNCTIONS");
            engine.setBaseUrl("");
            engine.setAuthType("IAM");
            engine.setDescription("AWS Step Functions state machine for PCA→COA pipeline. Requires aws-integration profile.");
            engine.setStatus("ACTIVE");
            repo.save(engine);
            log.info("Seeded default workflow engine: aws-step-functions");
        }
    }

    public void delete(Long id) {
        log.info("Deleting workflow engine id={}", id);
        repo.deleteById(id);
    }

    private Map<String, Object> toDto(WorkflowEngineMasterEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("engineName", e.getEngineName());
        m.put("engineType", e.getEngineType());
        m.put("baseUrl", e.getBaseUrl());
        m.put("authType", e.getAuthType());
        m.put("status", e.getStatus());
        m.put("description", e.getDescription());
        m.put("createdAt", e.getCreatedAt());
        m.put("updatedAt", e.getUpdatedAt());
        return m;
    }
}
