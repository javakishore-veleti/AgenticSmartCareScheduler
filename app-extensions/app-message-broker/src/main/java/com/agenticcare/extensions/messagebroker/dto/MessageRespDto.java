package com.agenticcare.extensions.messagebroker.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

@Data
@NoArgsConstructor
public class MessageRespDto {
    private Long id;
    private Long sequenceNumber;
    private String queueName;
    private String messageKey;
    private String payload;
    private LinkedHashMap<String, String> ctxData;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
