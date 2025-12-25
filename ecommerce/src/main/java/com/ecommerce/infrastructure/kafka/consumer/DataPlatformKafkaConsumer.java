package com.ecommerce.infrastructure.kafka.consumer;

import com.ecommerce.domain.order.event.OrderCreatedEvent;
import com.ecommerce.infrastructure.external.DataPlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformKafkaConsumer {

    private final DataPlatformService dataPlatformService;

    @KafkaListener(topics = "ecommerce.order.event.created.v1", groupId = "ecommerce-data-platform")
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            log.info("카프카 주문 이벤트 수신: userId={}, orderId={}", event.userId(), event.orderId());
            dataPlatformService.sendOrderData(event);
            log.info("데이터 플랫폼 전송 완료: userId={}, orderId={}", event.userId(), event.orderId());
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패: userId={}, orderId={}", event.userId(), event.orderId(), e);
            throw e;
        }
    }
}
