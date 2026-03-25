package com.agenticcare.common.dto.admin;

import com.agenticcare.common.enums.DatasetFormat;
import com.agenticcare.common.enums.DatasetStorageType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DatasetIngestReqDto {
    private String datasetCode;
    private DatasetStorageType storageType;
    private DatasetFormat format;
    private String storageLocationHint;
}
