package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.DatasetMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatasetMasterRepository extends JpaRepository<DatasetMasterEntity, Long> {
    Optional<DatasetMasterEntity> findByDatasetCode(String datasetCode);
}
