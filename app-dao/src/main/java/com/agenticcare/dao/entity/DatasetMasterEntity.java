package com.agenticcare.dao.entity;

import com.agenticcare.common.enums.DatasetFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "datasets_master", schema = "smartcare_admin_db")
@Data
@NoArgsConstructor
public class DatasetMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_code", unique = true, nullable = false)
    private String datasetCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "source_provider")
    private String sourceProvider;

    @Column(name = "license_type")
    private String licenseType;

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "column_count")
    private Integer columnCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_format")
    private DatasetFormat defaultFormat;

    @Column(name = "tags")
    private String tags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
