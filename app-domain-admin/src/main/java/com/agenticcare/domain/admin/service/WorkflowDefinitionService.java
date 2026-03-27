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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(Long id) {
        return toDto(repo.findById(id).orElseThrow(() -> new RuntimeException("Definition not found: " + id)));
    }

    public void seedDefaults() {
        seedOne("patient_outreach_orchestration", "Patient Outreach Orchestration",
                "End-to-end agentic pipeline: PCA assesses patient context and risk, "
                + "COA selects optimal channel (IVR/SMS/Callback) based on C_p state, "
                + "then executes live outreach via Connect or SNS. The core workflow of the system.",
                "PCA \u2192 COA (Full Pipeline)",
                "Bedrock Agents, Connect, SNS, EventBridge, Lambda",
                "Spring AI ChatClient, Bedrock Claude, @Tool for Connect/SNS",
                "V. Methodology, VII. Results");

        seedOne("smart_appointment_confirmation", "Smart Appointment Confirmation",
                "COA agent conducts live patient interaction: places IVR call or sends SMS deep-link, "
                + "interprets patient response using Bedrock Claude (confirm, reschedule, cancel), "
                + "updates appointment status, and escalates non-responders to callback queue.",
                "COA (Communication Orchestration Agent)",
                "Bedrock Agents, Connect Contact Flows, SNS, Lambda",
                "Spring AI ChatClient, Bedrock Claude, @Tool for response parsing",
                "V. Methodology \u2014 Section B");

        seedOne("waitlist_slot_fulfillment", "Waitlist Slot Fulfillment",
                "When PCA predicts a likely no-show (R_p > 0.65), RRA agent proactively identifies "
                + "waitlisted patients, uses Bedrock to rank candidates by urgency and travel feasibility, "
                + "then triggers COA to offer the slot via the patient's preferred channel.",
                "PCA \u2192 RRA \u2192 COA (Cross-Agent Coordination)",
                "Bedrock Agents, HealthLake (FHIR R4), Step Functions, Connect",
                "Spring AI ChatClient, Bedrock Claude, @Tool for HealthLake/StepFn",
                "VII. Results");

        seedOne("provider_schedule_optimization", "Provider Schedule Optimization",
                "RRA agent analyzes predicted no-show patterns across provider schedules, "
                + "uses Bedrock reasoning to recommend double-booking for high-risk slots, "
                + "buffer adjustments, and provider workload rebalancing. Publishes recommendations to PSA.",
                "PCA \u2192 RRA \u2192 PSA (Provider Scheduling Agent)",
                "Bedrock Agents, HealthLake (FHIR R4), EventBridge, Lambda",
                "Spring AI ChatClient, Bedrock Claude, @Tool for FHIR schedule queries",
                "VII. Results");

        seedOne("realtime_outreach_batch", "Real-Time Outreach Simulation",
                "Two-DAG agentic pipeline: DAG 1 (Batch) reads dataset and dispatches each patient async to "
                + "DAG 2 (Agent). DAG 2 runs PCA→COA with LLM reasoning per patient — assesses context, "
                + "selects channel, executes outreach, interprets response. Escalates to admin when needed. "
                + "30 agent instances run in parallel, simulating real-time urgency.",
                "PCA \u2192 COA \u2192 Admin Escalation (Agentic AI Pipeline)",
                "Bedrock Agents, Connect, SNS, EventBridge, Lambda",
                "Airflow AI SDK, @task.agent, @task.llm_branch, Bedrock Claude, Spring AI @Tool",
                "V. Methodology, VII. Results");

        seedOne("outreach_compliance_audit", "Outreach Compliance Audit",
                "ACA agent reviews all outreach interactions for HIPAA compliance, patient consent verification, "
                + "and communication frequency limits. Uses Bedrock to flag anomalies and generate "
                + "natural-language audit summaries. Logs to OpenSearch for dashboard visibility.",
                "ACA (Audit & Compliance Agent)",
                "Bedrock Agents, OpenSearch, S3, CloudWatch",
                "Spring AI ChatClient, Bedrock Claude, OpenSearch vector search",
                "VII. Discussion",
                false);  // does not require a dataset — audits existing outreach logs

        log.info("Seeded default workflow definitions");
    }

    private void seedOne(String key, String displayName, String description,
                         String agentPipeline, String awsServices, String techStack, String paperSection) {
        seedOne(key, displayName, description, agentPipeline, awsServices, techStack, paperSection, true);
    }

    private void seedOne(String key, String displayName, String description,
                         String agentPipeline, String awsServices, String techStack, String paperSection,
                         boolean requiresDataset) {
        if (repo.findByWorkflowKey(key).isPresent()) {
            log.info("Workflow definition already exists: {}", key);
            return;
        }
        WorkflowDefinitionMasterEntity entity = new WorkflowDefinitionMasterEntity();
        entity.setWorkflowKey(key);
        entity.setDisplayName(displayName);
        entity.setDescription(description);
        entity.setAgentPipeline(agentPipeline);
        entity.setAwsServices(awsServices);
        entity.setTechStack(techStack);
        entity.setPaperSection(paperSection);
        entity.setRequiresDataset(requiresDataset);
        entity.setStatus("ACTIVE");
        repo.save(entity);
        log.info("Seeded workflow definition: {}", key);
    }

    @Transactional
    public void seedDefaultMappings() {
        List<WorkflowDefinitionMasterEntity> allDefs = repo.findAll();
        List<WorkflowEngineMasterEntity> allEngines = engineRepo.findAll();

        for (WorkflowDefinitionMasterEntity def : allDefs) {
            for (WorkflowEngineMasterEntity engine : allEngines) {
                if (mappingRepo.findByWorkflowDefinitionIdAndEngineId(def.getId(), engine.getId()).isPresent()) {
                    continue;
                }
                {
                    WorkflowEngineMappingEntity mapping = new WorkflowEngineMappingEntity();
                    mapping.setWorkflowDefinition(def);
                    mapping.setEngine(engine);
                    mapping.setEngineWorkflowRef(def.getWorkflowKey());  // DAG id = workflow key
                    mapping.setStatus("ACTIVE");
                    mappingRepo.save(mapping);
                    log.info("Mapped workflow={} to engine={} ref={}", def.getWorkflowKey(), engine.getEngineName(), def.getWorkflowKey());
                }
            }
        }
        log.info("Seeded default workflow-engine mappings: {} definitions x {} engines", allDefs.size(), allEngines.size());
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

    @Transactional(readOnly = true)
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
        m.put("agentPipeline", e.getAgentPipeline());
        m.put("awsServices", e.getAwsServices());
        m.put("techStack", e.getTechStack());
        m.put("paperSection", e.getPaperSection());
        m.put("requiresDataset", e.getRequiresDataset());
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
