package com.agenticcare.extensions.messagebroker.service;

import com.agenticcare.extensions.messagebroker.dto.MessagePublishReqDto;
import com.agenticcare.extensions.messagebroker.dto.MessageRespDto;
import com.agenticcare.extensions.messagebroker.entity.MessageQueueEntity;
import java.util.Map;
import com.agenticcare.extensions.messagebroker.repository.MessageQueueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageBrokerService {

    private static final Logger log = LoggerFactory.getLogger(MessageBrokerService.class);
    private final MessageQueueRepository repo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageBrokerService(MessageQueueRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getTopics() {
        List<Object[]> rows = repo.countByQueueNameAndStatus();
        java.util.LinkedHashMap<String, Map<String, Object>> topicMap = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            String queue = (String) row[0];
            String status = (String) row[1];
            Long count = (Long) row[2];
            topicMap.computeIfAbsent(queue, k -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("topicName", k); m.put("pending", 0L); m.put("processing", 0L);
                m.put("completed", 0L); m.put("failed", 0L); m.put("total", 0L);
                return m;
            });
            Map<String, Object> t = topicMap.get(queue);
            t.put(status.toLowerCase(), count);
            t.put("total", (Long) t.get("total") + count);
        }
        return new java.util.ArrayList<>(topicMap.values());
    }

    public List<MessageRespDto> getMessagesByTopic(String queueName) {
        return repo.findByQueueNameOrderBySequenceNumberDesc(queueName).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public MessageRespDto publish(MessagePublishReqDto req) {
        Long nextSeq = repo.findMaxSequenceByQueueName(req.getQueueName()) + 1;

        MessageQueueEntity entity = new MessageQueueEntity();
        entity.setQueueName(req.getQueueName());
        entity.setSequenceNumber(nextSeq);
        entity.setMessageKey(req.getMessageKey());
        entity.setPayload(req.getPayload());
        try {
            entity.setCtxData(req.getCtxData() != null ? objectMapper.writeValueAsString(req.getCtxData()) : null);
        } catch (Exception e) {
            entity.setCtxData(null);
        }
        entity.setStatus("PENDING");

        entity = repo.save(entity);
        log.info("Published message to queue={} seq={} key={}", req.getQueueName(), nextSeq, req.getMessageKey());
        return toDto(entity);
    }

    public List<MessageRespDto> consume(String queueName, int maxMessages) {
        List<MessageQueueEntity> messages = repo.findByQueueNameAndStatusOrderBySequenceNumber(queueName, "PENDING");
        return messages.stream().limit(maxMessages).map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void acknowledge(Long messageId) {
        MessageQueueEntity entity = repo.findById(messageId).orElseThrow();
        entity.setStatus("COMPLETED");
        entity.setProcessedAt(LocalDateTime.now());
        repo.save(entity);
        log.info("Acknowledged message id={} queue={}", messageId, entity.getQueueName());
    }

    private MessageRespDto toDto(MessageQueueEntity entity) {
        MessageRespDto dto = new MessageRespDto();
        dto.setId(entity.getId());
        dto.setSequenceNumber(entity.getSequenceNumber());
        dto.setQueueName(entity.getQueueName());
        dto.setMessageKey(entity.getMessageKey());
        dto.setPayload(entity.getPayload());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setProcessedAt(entity.getProcessedAt());
        try {
            if (entity.getCtxData() != null) {
                dto.setCtxData(objectMapper.readValue(entity.getCtxData(), LinkedHashMap.class));
            }
        } catch (Exception ignored) {}
        return dto;
    }
}
