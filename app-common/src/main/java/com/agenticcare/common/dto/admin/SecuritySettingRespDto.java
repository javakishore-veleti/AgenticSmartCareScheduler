package com.agenticcare.common.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SecuritySettingRespDto {
    private Long id;
    private String settingName;
    private String settingType;
    private String summary;  // human-readable, no secrets
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
