package com.agenticcare.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_settings", schema = "smartcare_admin_db")
@Data
@NoArgsConstructor
public class SecuritySettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_name", unique = true, nullable = false)
    private String settingName;

    @Column(name = "setting_type", nullable = false)
    private String settingType;  // AWS_CLIENT_PROFILE, AWS_CLIENT_CREDENTIALS, AWS_SECRETS_MANAGER

    @Column(name = "configs_json", columnDefinition = "TEXT")
    private String configsJson;  // JSON blob with type-specific config

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

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
