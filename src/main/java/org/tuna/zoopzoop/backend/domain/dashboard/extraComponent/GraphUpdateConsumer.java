package org.tuna.zoopzoop.backend.domain.dashboard.extraComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.BodyForReactFlow;
import org.tuna.zoopzoop.backend.domain.dashboard.dto.GraphUpdateMessage;
import org.tuna.zoopzoop.backend.domain.dashboard.service.DashboardService;

@Slf4j
@Component
@RequiredArgsConstructor
public class GraphUpdateConsumer {
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "graph.update.queue")
    public void handleGraphUpdate(GraphUpdateMessage message) {
        log.info("Received graph update message for dashboardId: {}", message.dashboardId());
        try {
            BodyForReactFlow dto = objectMapper.readValue(message.requestBody(), BodyForReactFlow.class);
            dashboardService.updateGraph(message.dashboardId(), dto);
            log.info("Successfully updated graph for dashboardId: {}", message.dashboardId());
        } catch (ObjectOptimisticLockingFailureException e) {
            // Optimistic Lock 충돌 발생
            // 정상적인 상황으로 간주하고 무시 (DLQ 전송 안하고 큐에서 제거)
            log.warn("Stale update attempt for dashboardId: {}. A newer version already exists. Discarding message.", message.dashboardId());

        } catch (Exception e) {
            // 비정상적인 에러 발생
            // 메세지를 Requeue하지 않고 DLQ로 전송
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
