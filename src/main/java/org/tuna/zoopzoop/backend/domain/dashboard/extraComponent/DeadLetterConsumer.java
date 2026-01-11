package org.tuna.zoopzoop.backend.domain.dashboard.extraComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.FailedMessage;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.MessageStatus;
import org.tuna.zoopzoop.backend.domain.dashboard.repository.FailedMessageRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConsumer {
    private final FailedMessageRepository failedMessageRepository;

    @RabbitListener(queues = "graph.update.queue.dlq")
    public void processDeadLetter(Message message) {
        // 원본 큐 이름 추출
        String originalQueue = "unknown";
        List<? extends Map<String, ?>> xDeath = message.getMessageProperties().getXDeathHeader();

        if (xDeath != null && !xDeath.isEmpty())
            originalQueue = xDeath.get(0).get("queue").toString();

        // 실패 사유 추출
        String reason = (xDeath != null && !xDeath.isEmpty())
                ? xDeath.get(0).get("reason").toString()
                : "Unknown failure";

        // 로깅 & DB 저장

        log.error("##### DLQ 인입 발생: 원본 큐 [{}] #####", originalQueue);

        FailedMessage failedRecord = FailedMessage.builder()
                .queueName(originalQueue)
                .payload(new String(message.getBody()))
                .errorMessage("Reason: " + reason) // 필요시 헤더에서 에러 추출 가능
                .status(MessageStatus.PENDING)
                .build();

        failedMessageRepository.save(failedRecord);
        log.info("실패 메시지가 DB에 저장되었습니다. ID: {}", failedRecord.getId());
    }
}
