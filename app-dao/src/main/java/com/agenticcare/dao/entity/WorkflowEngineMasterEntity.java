package com.agenticcare.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_engine_master", schema = "smartcare_admin_db")
@Data
@NoArgsConstructor
public class WorkflowEngineMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "engine_name", unique = true, nullable = false)
    private String engineName;

    @Column(name = "engine_type", nullable = false)
    private String engineType;  // AIRFLOW, AWS_EMR, DATABRICKS, AWS_STEP_FUNCTIONS

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "auth_type")
    private String authType;  // BASIC, TOKEN, IAM

    @Column(name = "configs_json", columnDefinition = "TEXT")
    private String configsJson;

    @Column(name = "status", nullable = false)
    private String status;  // ACTIVE, INACTIVE

    @Column(name = "description")
    private String description;

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
