package com.ecommerce.application.event;

import com.ecommerce.domain.order.event.OrderCreatedEvent;
import com.ecommerce.infrastructure.external.DataPlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventListener {

    private final DataPlatformService dataPlatformService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            log.info("리스너 주문 생성 이벤트 수신: orderId={}, userId={}", event.orderId(), event.userId());

            dataPlatformService.sendOrderData(event);

            log.info("리스너 데이터 플랫폼 전송 완료: orderId={}, userId={}", event.orderId(), event.userId());

        } catch (Exception e) {
            log.error("리스너 데이터 플랫폼 전송 실패: orderId={}, userId={}, error={}",
                    event.orderId(), event.userId(), e.getMessage(), e);
        }
    }
}
