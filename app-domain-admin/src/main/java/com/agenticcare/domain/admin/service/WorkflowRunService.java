package com.agenticcare.domain.admin.service;

import com.agenticcare.dao.entity.DatasetInstanceEntity;
import com.agenticcare.dao.entity.WorkflowDefinitionMasterEntity;
import com.agenticcare.dao.entity.WorkflowEngineMasterEntity;
import com.agenticcare.dao.entity.WorkflowRunEntity;
import com.agenticcare.dao.repository.DatasetInstanceRepository;
import com.agenticcare.dao.repository.WorkflowDefinitionMasterRepository;
import com.agenticcare.dao.repository.WorkflowEngineMasterRepository;
import com.agenticcare.dao.repository.WorkflowRunRepository;
import com.agenticcare.wfs.executor.WfEngineFacade;
import com.agenticcare.wfs.executor.WfEngineFacadeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkflowRunService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRunService.class);
    private final WorkflowRunRepository repo;
    private final WorkflowDefinitionMasterRepository defRepo;
    private final WorkflowEngineMasterRepository engineRepo;
    private final DatasetInstanceRepository datasetInstanceRepo;
    private final WfEngineFacadeFactory facadeFactory;

    public WorkflowRunService(WorkflowRunRepository repo,
                              WorkflowDefinitionMasterRepository defRepo,
                              WorkflowEngineMasterRepository engineRepo,
                              DatasetInstanceRepository datasetInstanceRepo,
                              WfEngineFacadeFactory facadeFactory) {
        this.repo = repo;
        this.defRepo = defRepo;
        this.engineRepo = engineRepo;
        this.datasetInstanceRepo = datasetInstanceRepo;
        this.facadeFactory = facadeFactory;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(Long id) {
        return toDto(repo.findById(id).orElseThrow(() -> new RuntimeException("Run not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getByDefinitionId(Long defId) {
        return repo.findByWorkflowDefinitionIdOrderByCreatedAtDesc(defId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getByDatasetInstanceId(Long instanceId) {
        return repo.findByDatasetInstanceIdOrderByCreatedAtDesc(instanceId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> submit(Map<String, Object> req) {
        Long defId = ((Number) req.get("workflowDefinitionId")).longValue();
        Long engineId = ((Number) req.get("engineId")).longValue();

        WorkflowDefinitionMasterEntity def = defRepo.findById(defId)
                .orElseThrow(() -> new RuntimeException("Definition not found: " + defId));
        WorkflowEngineMasterEntity engine = engineRepo.findById(engineId)
                .orElseThrow(() -> new RuntimeException("Engine not found: " + engineId));

        WorkflowRunEntity entity = new WorkflowRunEntity();
        entity.setWorkflowDefinition(def);
        entity.setEngine(engine);
        entity.setRunStatus("SUBMITTED");
        entity.setParametersJson(req.containsKey("parametersJson") ? (String) req.get("parametersJson") : null);

        if (req.containsKey("datasetInstanceId") && req.get("datasetInstanceId") != null) {
            Long instanceId = ((Number) req.get("datasetInstanceId")).longValue();
            DatasetInstanceEntity instance = datasetInstanceRepo.findById(instanceId)
                    .orElseThrow(() -> new RuntimeException("Dataset instance not found: " + instanceId));
            entity.setDatasetInstance(instance);
        }

        entity = repo.save(entity);
        log.info("Submitted workflow run id={} definition={} engine={}",
                entity.getId(), def.getWorkflowKey(), engine.getEngineName());

        // Build parameters for engine using portable ~/runtime_data paths
        // Docker mounts ${HOME}/runtime_data → /home/airflow/runtime_data
        // So ~/runtime_data/... works on both host and container after path resolution
        Map<String, Object> engineParams = new LinkedHashMap<>();
        if (entity.getDatasetInstance() != null) {
            engineParams.put("datasetPath", entity.getDatasetInstance().getStorageLocationHint());
            engineParams.put("datasetInstanceId", entity.getDatasetInstance().getId());
        }
        engineParams.put("workflowKey", def.getWorkflowKey());
        engineParams.put("workflowRunId", entity.getId());
        engineParams.put("outputDir", "~/runtime_data/workflow_output/run_" + entity.getId());

        // Trigger async execution via the engine facade
        executeAsync(entity.getId(), engine.getEngineType(), engine.getBaseUrl(),
                def.getWorkflowKey(), engineParams);

        return toDto(entity);
    }

    @Async
    public void executeAsync(Long runId, String engineType, String engineBaseUrl,
                             String workflowRef, Map<String, Object> parameters) {
        try {
            WfEngineFacade facade = facadeFactory.getFacade(engineType);

            // Task 1: Trigger the workflow on the engine
            String externalRunId = facade.triggerRun(engineBaseUrl, workflowRef, runId, parameters);

            // Task 2: Update status to RUNNING
            updateStatus(runId, "RUNNING", externalRunId, null, null);
            log.info("Workflow run id={} now RUNNING, externalRunId={}", runId, externalRunId);

        } catch (UnsupportedOperationException e) {
            // Engine not yet implemented — leave as SUBMITTED
            log.warn("Engine facade not implemented: {}. Run id={} stays SUBMITTED.", engineType, runId);
        } catch (Exception e) {
            // Engine trigger failed
            updateStatus(runId, "FAILED", null, null, e.getMessage());
            log.error("Workflow run id={} FAILED: {}", runId, e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String status, String externalRunId,
                                             String resultPath, String errorMessage) {
        WorkflowRunEntity entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Run not found: " + id));
        entity.setRunStatus(status);
        if (externalRunId != null) entity.setExternalRunId(externalRunId);
        if (resultPath != null) entity.setResultPath(resultPath);
        if (errorMessage != null) entity.setErrorMessage(errorMessage);
        if ("RUNNING".equals(status)) entity.setStartedAt(LocalDateTime.now());
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) entity.setCompletedAt(LocalDateTime.now());
        log.info("Updated workflow run id={} status={}", id, status);
        return toDto(repo.save(entity));
    }

    private Map<String, Object> toDto(WorkflowRunEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("workflowDefinitionId", e.getWorkflowDefinition().getId());
        m.put("workflowKey", e.getWorkflowDefinition().getWorkflowKey());
        m.put("workflowDisplayName", e.getWorkflowDefinition().getDisplayName());
        m.put("engineId", e.getEngine().getId());
        m.put("engineName", e.getEngine().getEngineName());
        m.put("engineType", e.getEngine().getEngineType());
        m.put("datasetInstanceId", e.getDatasetInstance() != null ? e.getDatasetInstance().getId() : null);
        m.put("runStatus", e.getRunStatus());
        m.put("externalRunId", e.getExternalRunId());
        m.put("parametersJson", e.getParametersJson());
        m.put("resultPath", e.getResultPath());
        m.put("errorMessage", e.getErrorMessage());
        m.put("submittedAt", e.getSubmittedAt());
        m.put("startedAt", e.getStartedAt());
        m.put("completedAt", e.getCompletedAt());
        return m;
    }
}
