package com.agenticcare.common.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DatasetIngestReqDto {
    private String storageType;      // LOCAL_FILESYSTEM, AWS_S3
    private String localBasePath;    // for LOCAL_FILESYSTEM
    private String s3Bucket;         // for AWS_S3
    private String s3Prefix;         // for AWS_S3
    private String awsRegion;        // for AWS_S3
}
