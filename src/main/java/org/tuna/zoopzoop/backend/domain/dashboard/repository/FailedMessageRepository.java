package org.tuna.zoopzoop.backend.domain.dashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.Dashboard;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.FailedMessage;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.MessageStatus;

import java.util.List;

@Repository
public interface FailedMessageRepository extends JpaRepository<FailedMessage, Integer> {
    List<FailedMessage> findByStatus(MessageStatus status);
    List<FailedMessage> findByQueueName(String queueName);
}