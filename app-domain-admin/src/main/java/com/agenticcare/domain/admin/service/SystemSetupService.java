package com.agenticcare.domain.admin.service;

import com.agenticcare.dao.entity.SystemSettingsLogEntity;
import com.agenticcare.dao.repository.SystemSettingsLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemSetupService {

    private static final Logger log = LoggerFactory.getLogger(SystemSetupService.class);
    private final SystemSettingsLogRepository logRepo;

    public static final String DATASETS_SEEDED = "datasets_seeded";
    public static final String WORKFLOWS_SEEDED = "workflows_seeded";
    public static final String ENGINES_SEEDED = "engines_seeded";

    public SystemSetupService(SystemSettingsLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    public Map<String, Object> getSetupStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        boolean datasetsSeeded = isSeeded(DATASETS_SEEDED);
        boolean workflowsSeeded = isSeeded(WORKFLOWS_SEEDED);
        boolean enginesSeeded = isSeeded(ENGINES_SEEDED);

        status.put("datasetsSeeded", datasetsSeeded);
        status.put("workflowsSeeded", workflowsSeeded);
        status.put("enginesSeeded", enginesSeeded);
        status.put("allSeeded", datasetsSeeded && workflowsSeeded && enginesSeeded);

        List<Map<String, String>> pending = new ArrayList<>();
        if (!datasetsSeeded) pending.add(Map.of(
                "key", DATASETS_SEEDED,
                "label", "Default Datasets",
                "description", "Medical Appointment No-Show dataset definition",
                "action", "seed-datasets"));
        if (!workflowsSeeded) pending.add(Map.of(
                "key", WORKFLOWS_SEEDED,
                "label", "Workflow Definitions",
                "description", "5 agentic outreach workflows (Patient Outreach, Confirmation, Waitlist, Schedule, Audit)",
                "action", "seed-workflows"));
        if (!enginesSeeded) pending.add(Map.of(
                "key", ENGINES_SEEDED,
                "label", "Workflow Engines",
                "description", "Local Apache Airflow engine registration",
                "action", "seed-engines"));
        status.put("pendingSetup", pending);

        return status;
    }

    public boolean isSeeded(String settingKey) {
        return logRepo.findFirstBySettingKeyAndActivityType(settingKey, "SEED").isPresent();
    }

    public List<SystemSettingsLogEntity> getAllLogs() {
        return logRepo.findAll();
    }

    public void recordSeed(String settingKey, String details) {
        SystemSettingsLogEntity entry = new SystemSettingsLogEntity();
        entry.setSettingKey(settingKey);
        entry.setActivityType("SEED");
        entry.setDetails(details);
        logRepo.save(entry);
        log.info("Recorded seed activity: {} — {}", settingKey, details);
    }
}
