package com.ecommerce.domain.order.event;


import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal pointBalance,
        List<Long> cartItemIds
) {
}
