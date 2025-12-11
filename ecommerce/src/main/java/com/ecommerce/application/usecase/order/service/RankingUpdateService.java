package com.ecommerce.application.usecase.order.service;

import com.ecommerce.domain.order.OrderItem;
import com.ecommerce.infrastructure.redis.ProductRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingUpdateService {

    private final ProductRankingRepository productRankingRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRanking(Long orderId, List<OrderItem> orderItems) {
        Map<Long, Integer> productQuantities = orderItems.stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum  // 같은 상품이 여러 번 있으면 합산
                ));

        try {
            productRankingRepository.incrementTodayRanking(productQuantities);
        } catch (Exception e) {
            log.error("랭킹 업데이트 실패 - orderId: {}", orderId, e);
        }
    }
}
