package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.point.PointHistory;
import com.ecommerce.infrastructure.repository.PointHistoryRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * PointHistory 인메모리 Repository 구현체
 */
@Repository
@Profile("memory")
public class InMemoryPointHistoryRepository implements PointHistoryRepository {
    private final Map<Long, PointHistory> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public PointHistory save(PointHistory pointHistory) {
        Long id = pointHistory.getId();
        if (id == null) {
            id = idGenerator.getAndIncrement();
        }

        PointHistory savedHistory = new PointHistory(
            id,
            pointHistory.getUserId(),
            pointHistory.getPointAmount(),
            pointHistory.getTransactionType(),
            pointHistory.getBalanceAfter(),
            pointHistory.getOrderId(),
            pointHistory.getDescription()
        );

        store.put(id, savedHistory);
        return savedHistory;
    }

    @Override
    public List<PointHistory> findByUserId(Long userId) {
        return store.values().stream()
            .filter(history -> history.getUserId().equals(userId))
            .sorted((h1, h2) -> {
                // createdAt이 같으면 ID로 내림차순 정렬 (최신순)
                int timeCompare = h2.getCreatedAt().compareTo(h1.getCreatedAt());
                if (timeCompare == 0) {
                    return h2.getId().compareTo(h1.getId());
                }
                return timeCompare;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<PointHistory> findByUserIdWithPagination(Long userId, int offset, int limit) {
        return store.values().stream()
            .filter(history -> history.getUserId().equals(userId))
            .sorted((h1, h2) -> {
                // createdAt이 같으면 ID로 내림차순 정렬 (최신순)
                int timeCompare = h2.getCreatedAt().compareTo(h1.getCreatedAt());
                if (timeCompare == 0) {
                    return h2.getId().compareTo(h1.getId());
                }
                return timeCompare;
            })
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
    }

    // 테스트용 메서드
    public void clear() {
        store.clear();
        idGenerator.set(1);
    }
}
