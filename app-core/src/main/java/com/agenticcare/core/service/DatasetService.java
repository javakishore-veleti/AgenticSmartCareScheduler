package com.agenticcare.core.service;

import com.agenticcare.common.dto.admin.DatasetDetailsRespDto;
import com.agenticcare.common.enums.DatasetFormat;
import com.agenticcare.common.enums.DatasetStatus;
import com.agenticcare.common.enums.DatasetStorageType;
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

    @PostConstruct
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
