package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.WorkflowEngineMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowEngineMappingRepository extends JpaRepository<WorkflowEngineMappingEntity, Long> {
    List<WorkflowEngineMappingEntity> findByWorkflowDefinitionId(Long workflowDefinitionId);
    List<WorkflowEngineMappingEntity> findByEngineId(Long engineId);
    List<WorkflowEngineMappingEntity> findByWorkflowDefinitionIdAndStatus(Long workflowDefinitionId, String status);
}
