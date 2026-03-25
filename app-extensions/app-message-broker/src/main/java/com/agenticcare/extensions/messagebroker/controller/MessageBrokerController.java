package com.agenticcare.extensions.messagebroker.controller;

import com.agenticcare.extensions.messagebroker.dto.MessagePublishReqDto;
import com.agenticcare.extensions.messagebroker.dto.MessageRespDto;
import com.agenticcare.extensions.messagebroker.service.MessageBrokerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/smart-care/api/broker/v1/messages")
public class MessageBrokerController {

    private final MessageBrokerService service;

    public MessageBrokerController(MessageBrokerService service) {
        this.service = service;
    }

    @GetMapping("/topics")
    public ResponseEntity<List<Map<String, Object>>> getTopics() {
        return ResponseEntity.ok(service.getTopics());
    }

    @GetMapping("/topic/{queueName}")
    public ResponseEntity<List<MessageRespDto>> getMessagesByTopic(@PathVariable String queueName) {
        return ResponseEntity.ok(service.getMessagesByTopic(queueName));
    }

    @PostMapping("/publish")
    public ResponseEntity<MessageRespDto> publish(@RequestBody MessagePublishReqDto req) {
        return ResponseEntity.ok(service.publish(req));
    }

    @GetMapping("/consume/{queueName}")
    public ResponseEntity<List<MessageRespDto>> consume(@PathVariable String queueName,
                                                         @RequestParam(defaultValue = "10") int max) {
        return ResponseEntity.ok(service.consume(queueName, max));
    }

    @PostMapping("/{messageId}/acknowledge")
    public ResponseEntity<Map<String, String>> acknowledge(@PathVariable Long messageId) {
        service.acknowledge(messageId);
        return ResponseEntity.ok(Map.of("status", "acknowledged"));
    }
}
