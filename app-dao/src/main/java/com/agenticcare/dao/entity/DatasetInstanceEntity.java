package com.agenticcare.dao.entity;

import com.agenticcare.common.enums.DatasetFormat;
import com.agenticcare.common.enums.DatasetStatus;
import com.agenticcare.common.enums.DatasetStorageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_instance", schema = "smartcare_admin_db")
@Data
@NoArgsConstructor
public class DatasetInstanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_name")
    private String instanceName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_master_id", nullable = false)
    private DatasetMasterEntity datasetMaster;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false)
    private DatasetStorageType storageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private DatasetFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DatasetStatus status;

    @Column(name = "storage_location_hint")
    private String storageLocationHint;

    @Column(name = "is_multi_file")
    private Boolean isMultiFile;

    @Column(name = "has_subfolders")
    private Boolean hasSubfolders;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "loaded_record_count")
    private Long loadedRecordCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastVerifiedAt = LocalDateTime.now();
    }
}
