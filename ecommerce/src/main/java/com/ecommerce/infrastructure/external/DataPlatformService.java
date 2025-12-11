package com.ecommerce.infrastructure.external;

import com.ecommerce.domain.order.event.OrderCreatedEvent;

public interface DataPlatformService {
    boolean sendOrderData(OrderCreatedEvent event);
}
