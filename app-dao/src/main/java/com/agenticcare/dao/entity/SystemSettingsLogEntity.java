package com.agenticcare.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings_log", schema = "smartcare_admin_db")
@Data
@NoArgsConstructor
public class SystemSettingsLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false)
    private String settingKey;  // e.g. datasets_seeded, workflows_seeded, engines_seeded

    @Column(name = "activity_type", nullable = false)
    private String activityType;  // SEED, UPDATE, DELETE, RESET

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (performedBy == null) performedBy = "admin";
    }
}
