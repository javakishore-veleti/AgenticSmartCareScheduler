package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.SystemSettingsLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingsLogRepository extends JpaRepository<SystemSettingsLogEntity, Long> {
    List<SystemSettingsLogEntity> findBySettingKey(String settingKey);
    Optional<SystemSettingsLogEntity> findFirstBySettingKeyAndActivityType(String settingKey, String activityType);
}
