package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.AgenticOutreachActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgenticOutreachActionRepository extends JpaRepository<AgenticOutreachActionEntity, Long> {
    List<AgenticOutreachActionEntity> findByWorkflowRunIdOrderByCreatedAtDesc(Long workflowRunId);
    Optional<AgenticOutreachActionEntity> findByActionKey(String actionKey);
    List<AgenticOutreachActionEntity> findByPatientId(String patientId);
    List<AgenticOutreachActionEntity> findByActionStatus(String actionStatus);
}
