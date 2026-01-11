package org.tuna.zoopzoop.backend.domain.dashboard.extraComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;
import org.tuna.zoopzoop.backend.domain.dashboard.entity.FailedMessage;
import org.tuna.zoopzoop.backend.domain.dashboard.enums.MessageStatus;
import org.tuna.zoopzoop.backend.domain.dashboard.repository.FailedMessageRepository;

import java.util.HashMap;
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
        String payload = new String(message.getBody());

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
                .payload(payload)
                .errorMessage("Reason: " + reason) // 필요시 헤더에서 에러 추출 가능
                .status(MessageStatus.PENDING)
                .build();

        failedMessageRepository.save(failedRecord);
        log.info("실패 메시지가 DB에 저장되었습니다. ID: {}", failedRecord.getId());

        // sentry 전송
        sendToSentry(payload, reason, failedRecord.getId());
    }

    private void sendToSentry(String payload, String reason, Long dbRecordId) {
        Sentry.withScope(scope -> {
            // 필터링을 위한 태그 추가
            scope.setTag("queue.type", "DLQ");
            scope.setTag("failure.reason", reason);
            scope.setTag("failed.message.id", String.valueOf(dbRecordId));

            // 메시지 본문 추가
            Map<String, Object> data = new HashMap<>();
            data.put("payload", payload);
            scope.setContexts("Message Details", data);

            // 에러 이벤트 전송
            Sentry.captureMessage("RabbitMQ Message moved to DLQ: " + reason, SentryLevel.ERROR);
        });

        log.info("Sentry로 해당 장애 내용이 전송되었습니다.");
    }
}
