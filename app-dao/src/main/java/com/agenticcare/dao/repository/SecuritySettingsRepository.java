package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.SecuritySettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecuritySettingsRepository extends JpaRepository<SecuritySettingsEntity, Long> {
    Optional<SecuritySettingsEntity> findBySettingName(String settingName);
    List<SecuritySettingsEntity> findBySettingType(String settingType);
}
