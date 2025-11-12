package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.coupon.UserCoupon;
import com.ecommerce.infrastructure.repository.UserCouponRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * UserCoupon InMemory Repository 구현체
 * - ConcurrentHashMap 기반 인메모리 저장소
 */
@Repository
public class InMemoryUserCouponRepository implements UserCouponRepository {
    private final Map<Long, UserCoupon> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return store.values().stream()
            .filter(coupon -> coupon.getUserId().equals(userId))
            .sorted(Comparator.comparing(UserCoupon::getIssuedAt).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndCouponEventId(Long userId, Long couponEventId) {
        return store.values().stream()
            .anyMatch(coupon ->
                coupon.getUserId().equals(userId) &&
                coupon.getCouponEventId().equals(couponEventId)
            );
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        if (userCoupon.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            UserCoupon newCoupon = new UserCoupon(
                newId,
                userCoupon.getUserId(),
                userCoupon.getCouponEventId(),
                userCoupon.getStartDate(),
                userCoupon.getEndDate()
            );
            store.put(newId, newCoupon);
            return newCoupon;
        } else {
            store.put(userCoupon.getId(), userCoupon);
            return userCoupon;
        }
    }
}
