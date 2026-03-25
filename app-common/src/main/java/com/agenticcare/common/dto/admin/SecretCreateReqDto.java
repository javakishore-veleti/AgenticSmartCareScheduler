package com.agenticcare.common.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SecretCreateReqDto {
    private String secretName;
    private String secretType;      // AWS_CLIENT_PROFILE, AWS_CLIENT_CREDENTIALS, AWS_SECRETS_MANAGER
    private String awsProfileName;  // for AWS_CLIENT_PROFILE
    private String awsAccessKeyId;  // for AWS_CLIENT_CREDENTIALS
    private String awsSecretAccessKey; // for AWS_CLIENT_CREDENTIALS
    private String awsRegion;
}
