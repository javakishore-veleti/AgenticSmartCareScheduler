package com.agenticcare.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_definition_master", schema = "smartcare_admin_db")
@Data
@NoArgsConstructor
public class WorkflowDefinitionMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_key", unique = true, nullable = false)
    private String workflowKey;  // e.g. cp_simulation, model_retrain, data_quality_check

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "parameters_schema", columnDefinition = "TEXT")
    private String parametersSchema;  // JSON schema for workflow parameters

    @Column(name = "status", nullable = false)
    private String status;  // ACTIVE, INACTIVE

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
