package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.WorkflowEngineMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowEngineMasterRepository extends JpaRepository<WorkflowEngineMasterEntity, Long> {
    Optional<WorkflowEngineMasterEntity> findByEngineName(String engineName);
    List<WorkflowEngineMasterEntity> findByEngineType(String engineType);
    List<WorkflowEngineMasterEntity> findByStatus(String status);
}
