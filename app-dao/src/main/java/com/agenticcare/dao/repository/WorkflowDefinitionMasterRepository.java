package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.WorkflowDefinitionMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionMasterRepository extends JpaRepository<WorkflowDefinitionMasterEntity, Long> {
    Optional<WorkflowDefinitionMasterEntity> findByWorkflowKey(String workflowKey);
    List<WorkflowDefinitionMasterEntity> findByStatus(String status);
}
