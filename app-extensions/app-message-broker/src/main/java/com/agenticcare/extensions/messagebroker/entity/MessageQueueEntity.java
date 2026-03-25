package com.agenticcare.extensions.messagebroker.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_queue", schema = "messaging_broker")
@Data
@NoArgsConstructor
public class MessageQueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_number")
    private Long sequenceNumber;

    @Column(name = "queue_name", nullable = false)
    private String queueName;

    @Column(name = "message_key")
    private String messageKey;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "ctx_data", columnDefinition = "TEXT")
    private String ctxData;

    @Column(name = "status", nullable = false)
    private String status;  // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
