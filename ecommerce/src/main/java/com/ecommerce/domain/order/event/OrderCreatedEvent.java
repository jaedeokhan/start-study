package com.ecommerce.domain.order.event;


import com.ecommerce.domain.order.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Long pointBalance,
        List<Long> cartItemIds
) {
}
