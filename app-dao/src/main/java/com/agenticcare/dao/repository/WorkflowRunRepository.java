package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.WorkflowRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRunRepository extends JpaRepository<WorkflowRunEntity, Long> {
    List<WorkflowRunEntity> findByWorkflowDefinitionIdOrderByCreatedAtDesc(Long workflowDefinitionId);
    List<WorkflowRunEntity> findByDatasetInstanceIdOrderByCreatedAtDesc(Long datasetInstanceId);
    List<WorkflowRunEntity> findByRunStatus(String runStatus);
    List<WorkflowRunEntity> findByExternalRunId(String externalRunId);
}
