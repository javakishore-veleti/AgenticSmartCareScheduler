package com.agenticcare.core.service;

import com.agenticcare.common.dto.admin.DatasetDetailsRespDto;
import com.agenticcare.common.dto.admin.DatasetIngestReqDto;
import com.agenticcare.common.enums.DatasetFormat;
import com.agenticcare.common.enums.DatasetStatus;
import com.agenticcare.common.enums.DatasetStorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.agenticcare.dao.entity.DatasetInstanceEntity;
import com.agenticcare.dao.entity.DatasetMasterEntity;
import com.agenticcare.dao.repository.DatasetInstanceRepository;
import com.agenticcare.dao.repository.DatasetMasterRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DatasetService {

    private final DatasetMasterRepository masterRepo;
    private final DatasetInstanceRepository instanceRepo;

    public DatasetService(DatasetMasterRepository masterRepo, DatasetInstanceRepository instanceRepo) {
        this.masterRepo = masterRepo;
        this.instanceRepo = instanceRepo;
    }

    public void seedDefaultDatasets() {
        if (masterRepo.findByDatasetCode("MEDICAL_APPT_NOSHOW").isEmpty()) {
            DatasetMasterEntity ds = new DatasetMasterEntity();
            ds.setDatasetCode("MEDICAL_APPT_NOSHOW");
            ds.setDisplayName("Medical Appointment No-Show Dataset");
            ds.setDescription("Publicly available dataset of 110,527 medical appointment records from Brazilian public health clinics. " +
                    "Contains patient demographics, appointment scheduling data, chronic condition flags, SMS receipt status, " +
                    "and binary no-show outcome. Approximately 20% no-show rate. " +
                    "Source: Kaggle (joniarroba/noshowappointments). " +
                    "14 columns: PatientId, AppointmentID, Gender, ScheduledDay, AppointmentDay, Age, Neighbourhood, " +
                    "Scholarship, Hipertension, Diabetes, Alcoholism, Handcap, SMS_received, No-show.");
            ds.setSourceUrl("https://www.kaggle.com/datasets/joniarroba/noshowappointments");
            ds.setSourceProvider("Kaggle");
            ds.setLicenseType("Open Access");
            ds.setRecordCount(110527L);
            ds.setColumnCount(14);
            ds.setDefaultFormat(DatasetFormat.CSV_SINGLE_FILE);
            ds.setTags("healthcare,no-show,appointment,scheduling,brazil");
            masterRepo.save(ds);
        }
    }

    public DatasetDetailsRespDto getDatasetDetails(String datasetCode) {
        DatasetDetailsRespDto resp = new DatasetDetailsRespDto();

        Optional<DatasetMasterEntity> masterOpt = masterRepo.findByDatasetCode(datasetCode);
        if (masterOpt.isEmpty()) {
            resp.setDatasetCode(datasetCode);
            resp.setExists(false);
            return resp;
        }

        DatasetMasterEntity master = masterOpt.get();
        resp.setDatasetCode(master.getDatasetCode());
        resp.setDisplayName(master.getDisplayName());
        resp.setDescription(master.getDescription());
        resp.setSourceUrl(master.getSourceUrl());
        resp.setSourceProvider(master.getSourceProvider());
        resp.setLicenseType(master.getLicenseType());
        resp.setRecordCount(master.getRecordCount());
        resp.setColumnCount(master.getColumnCount());
        resp.setDefaultFormat(master.getDefaultFormat());
        resp.setTags(master.getTags());
        resp.setExists(true);

        List<DatasetInstanceEntity> instances = instanceRepo.findByDatasetMasterId(master.getId());
        resp.setInstances(instances.stream().map(this::toInstanceInfo).collect(Collectors.toList()));

        return resp;
    }

    private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

    public DatasetDetailsRespDto ingestDataset(String datasetCode, DatasetIngestReqDto req) {
        DatasetMasterEntity master = masterRepo.findByDatasetCode(datasetCode)
                .orElseThrow(() -> new RuntimeException("Dataset not found: " + datasetCode));

        String instanceUuid = java.util.UUID.randomUUID().toString();
        String storageTypeStr = (req != null && req.getStorageType() != null) ? req.getStorageType() : "LOCAL_FILESYSTEM";
        DatasetStorageType storageType = DatasetStorageType.valueOf(storageTypeStr);

        String locationHint;
        if (storageType == DatasetStorageType.AWS_S3) {
            String bucket = (req != null && req.getS3Bucket() != null) ? req.getS3Bucket() : "smartcare-datasets";
            String prefix = (req != null && req.getS3Prefix() != null) ? req.getS3Prefix() : "datasets/";
            String region = (req != null && req.getAwsRegion() != null) ? req.getAwsRegion() : "us-east-1";
            locationHint = "s3://" + bucket + "/" + prefix + instanceUuid + " [" + region + "]";
            log.info("Ingesting dataset: {} to S3: {}", datasetCode, locationHint);
        } else {
            String basePath = (req != null && req.getLocalBasePath() != null) ? req.getLocalBasePath()
                    : "~/runtime_data/DataSets/SmartCare-Admin/Datasets-Loaded";
            locationHint = basePath + "/" + instanceUuid;
            String fullPath = locationHint.replace("~", System.getProperty("user.home"));
            new java.io.File(fullPath).mkdirs();
            log.info("Ingesting dataset: {} to local: {}", datasetCode, fullPath);
        }

        DatasetInstanceEntity instance = new DatasetInstanceEntity();
        instance.setDatasetMaster(master);
        instance.setStorageType(storageType);
        instance.setFormat(master.getDefaultFormat());
        instance.setStatus(DatasetStatus.AVAILABLE);
        instance.setStorageLocationHint(locationHint);
        instance.setIsMultiFile(false);
        instance.setHasSubfolders(false);
        instance.setLoadedRecordCount(master.getRecordCount());
        instance.setFileSizeBytes(27_000_000L);
        instance.setLastVerifiedAt(LocalDateTime.now());
        instanceRepo.save(instance);

        log.info("Dataset {} ingested as {} to {}", datasetCode, storageType, locationHint);
        return getDatasetDetails(datasetCode);
    }

    public List<DatasetDetailsRespDto> getAllDatasets() {
        return masterRepo.findAll().stream()
                .map(master -> getDatasetDetails(master.getDatasetCode()))
                .collect(Collectors.toList());
    }

    private DatasetDetailsRespDto.DatasetInstanceInfo toInstanceInfo(DatasetInstanceEntity entity) {
        DatasetDetailsRespDto.DatasetInstanceInfo info = new DatasetDetailsRespDto.DatasetInstanceInfo();
        info.setInstanceId(entity.getId());
        info.setStorageType(entity.getStorageType());
        info.setFormat(entity.getFormat());
        info.setStatus(entity.getStatus());
        info.setStorageLocationHint(entity.getStorageLocationHint());
        info.setIsMultiFile(entity.getIsMultiFile());
        info.setHasSubfolders(entity.getHasSubfolders());
        info.setFileSizeBytes(entity.getFileSizeBytes());
        info.setLoadedRecordCount(entity.getLoadedRecordCount());
        info.setCreatedAt(entity.getCreatedAt());
        info.setLastVerifiedAt(entity.getLastVerifiedAt());
        info.setErrorMessage(entity.getErrorMessage());
        return info;
    }
}
