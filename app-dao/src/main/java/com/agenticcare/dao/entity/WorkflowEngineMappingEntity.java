package com.agenticcare.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_engine_mapping", schema = "smartcare_admin_db",
       uniqueConstraints = @UniqueConstraint(columnNames = {"workflow_definition_id", "engine_id"}))
@Data
@NoArgsConstructor
public class WorkflowEngineMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinitionMasterEntity workflowDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engine_id", nullable = false)
    private WorkflowEngineMasterEntity engine;

    @Column(name = "engine_workflow_ref")
    private String engineWorkflowRef;  // engine-specific ID: Airflow DAG id, EMR step name, etc.

    @Column(name = "configs_json", columnDefinition = "TEXT")
    private String configsJson;  // engine-specific config overrides

    @Column(name = "status", nullable = false)
    private String status;  // ACTIVE, INACTIVE

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }
}
