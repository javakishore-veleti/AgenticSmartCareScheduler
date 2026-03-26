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

    @Column(name = "agent_pipeline")
    private String agentPipeline;  // e.g. PCA, PCA → COA, PCA → RRA

    @Column(name = "aws_services")
    private String awsServices;  // e.g. Bedrock, SageMaker, S3

    @Column(name = "tech_stack")
    private String techStack;  // e.g. XGBoost, Spring AI, Python

    @Column(name = "paper_section")
    private String paperSection;  // e.g. VI. Evaluation, VII. Results

    @Column(name = "requires_dataset", nullable = false)
    private Boolean requiresDataset = true;

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
