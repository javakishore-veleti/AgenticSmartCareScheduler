package com.agenticcare.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "agentic_outreach_action", schema = "smartcare_admin_db")
@Data
@NoArgsConstructor
public class AgenticOutreachActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_key", unique = true, nullable = false, length = 100)
    private String actionKey;  // composite: {workflowRunId}_{engineInstanceId}_{patientId}

    @Column(name = "workflow_run_id", nullable = false)
    private Long workflowRunId;

    @Column(name = "workflow_engine_type")
    private String workflowEngineType;  // AIRFLOW, AWS_EMR, etc.

    @Column(name = "engine_instance_id")
    private String engineInstanceId;  // Airflow DAG run ID for this patient

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "context_state")
    private String contextState;  // REACHABLE_MOBILE, REACHABLE_STATIONARY, UNREACHABLE

    @Column(name = "channel_selected")
    private String channelSelected;  // VOICE_IVR, SMS_DEEPLINK, CALLBACK

    @Column(name = "patient_response")
    private String patientResponse;  // confirmed, rescheduled, no_answer, cancelled

    @Column(name = "action_status", nullable = false)
    private String actionStatus;  // ACTION_TAKEN, SKIPPED, ESCALATED_TO_ADMIN

    @Column(name = "action_detail_json", columnDefinition = "TEXT")
    private String actionDetailJson;  // full JSON: PCA reasoning, COA decision, why action taken/skipped

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (createdBy == null) createdBy = "agentic-agent";
    }
}
