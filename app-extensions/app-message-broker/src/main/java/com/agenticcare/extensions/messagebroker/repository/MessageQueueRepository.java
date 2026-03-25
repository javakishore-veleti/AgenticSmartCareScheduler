package com.agenticcare.extensions.messagebroker.repository;

import com.agenticcare.extensions.messagebroker.entity.MessageQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageQueueRepository extends JpaRepository<MessageQueueEntity, Long> {
    List<MessageQueueEntity> findByQueueNameAndStatusOrderBySequenceNumber(String queueName, String status);

    @Query("SELECT COALESCE(MAX(m.sequenceNumber), 0) FROM MessageQueueEntity m WHERE m.queueName = :queueName")
    Long findMaxSequenceByQueueName(String queueName);
}
