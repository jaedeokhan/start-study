package com.ecommerce.infrastructure.external;

import com.ecommerce.domain.order.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockDataPlatformService implements DataPlatformService {

    @Override
    public boolean sendOrderData(OrderCreatedEvent event) {

        try {
            log.info("데이터 플랫폼 전송 : orderId : {}, userId : {}, balance: {}, cartItemIds : {}",
                    event.orderId(), event.userId(), event.pointBalance(), event.cartItemIds());

            return true;
        } catch (Exception e) {
            log.error("[FAILED] 데이터 플랫폼 전송 실패", e);
            return false;
        }
    }
}
