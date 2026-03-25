package com.agenticcare.common.dto.admin;

import com.agenticcare.common.enums.DatasetFormat;
import com.agenticcare.common.enums.DatasetStatus;
import com.agenticcare.common.enums.DatasetStorageType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class DatasetDetailsRespDto {

    private String datasetCode;
    private String displayName;
    private String description;
    private String sourceUrl;
    private String sourceProvider;
    private String licenseType;
    private Long recordCount;
    private Integer columnCount;
    private DatasetFormat defaultFormat;
    private String tags;
    private boolean exists;
    private List<DatasetInstanceInfo> instances;

    @Data
    @NoArgsConstructor
    public static class DatasetInstanceInfo {
        private Long instanceId;
        private String instanceName;
        private DatasetStorageType storageType;
        private DatasetFormat format;
        private DatasetStatus status;
        private String storageLocationHint;
        private Boolean isMultiFile;
        private Boolean hasSubfolders;
        private Long fileSizeBytes;
        private Long loadedRecordCount;
        private LocalDateTime createdAt;
        private LocalDateTime lastVerifiedAt;
        private String errorMessage;
    }
}
