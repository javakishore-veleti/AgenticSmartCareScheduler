package com.agenticcare.domain.admin.service;

import com.agenticcare.dao.entity.SecuritySettingsEntity;
import com.agenticcare.dao.repository.SecuritySettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecuritySettingsService {

    private static final Logger log = LoggerFactory.getLogger(SecuritySettingsService.class);
    private final SecuritySettingsRepository repo;

    public SecuritySettingsService(SecuritySettingsRepository repo) {
        this.repo = repo;
    }

    public List<SecuritySettingsEntity> getAll() {
        return repo.findAll();
    }

    public List<SecuritySettingsEntity> getByType(String settingType) {
        return repo.findBySettingType(settingType);
    }

    public SecuritySettingsEntity create(SecuritySettingsEntity entity) {
        log.info("Creating security setting: {} type={}", entity.getSettingName(), entity.getSettingType());
        return repo.save(entity);
    }

    public void delete(Long id) {
        log.info("Deleting security setting id={}", id);
        repo.deleteById(id);
    }
}
