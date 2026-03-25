package com.agenticcare.common.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SecretDto {
    private Long id;
    private String secretName;
    private String secretType;
    private String awsProfileName;
    private String awsAccessKeyId;
    private String awsRegion;
    private String createdAt;
    // Note: awsSecretAccessKey is NEVER returned in responses
}
