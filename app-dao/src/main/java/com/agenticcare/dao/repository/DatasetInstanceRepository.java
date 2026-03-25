package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.DatasetInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetInstanceRepository extends JpaRepository<DatasetInstanceEntity, Long> {
    List<DatasetInstanceEntity> findByDatasetMasterId(Long datasetMasterId);
}
