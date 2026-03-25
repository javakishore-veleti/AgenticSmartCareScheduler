package com.agenticcare.extensions.messagebroker.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.LinkedHashMap;

@Data
@NoArgsConstructor
public class MessagePublishReqDto {
    private String queueName;
    private String messageKey;
    private String payload;
    private LinkedHashMap<String, String> ctxData;
}
