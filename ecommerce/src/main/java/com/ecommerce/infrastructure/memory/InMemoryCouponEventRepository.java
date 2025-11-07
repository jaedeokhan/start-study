package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.ecommerce.domain.coupon.exception.CouponEventNotFoundException;
import com.ecommerce.infrastructure.repository.CouponEventRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CouponEvent InMemory Repository 구현체
 * - ConcurrentHashMap 기반 인메모리 저장소
 * - ReentrantLock을 활용한 쿠폰 발급 동시성 제어
 */
@Repository
public class InMemoryCouponEventRepository implements CouponEventRepository {
    private final Map<Long, CouponEvent> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // ✅ 쿠폰 이벤트별 Lock 관리 (동시성 제어)
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<CouponEvent> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<CouponEvent> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public CouponEvent save(CouponEvent couponEvent) {
        if (couponEvent.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            // ID 설정은 외부에서 처리하거나, 생성자 호출 필요
            store.put(newId, couponEvent);
            return couponEvent;
        } else {
            store.put(couponEvent.getId(), couponEvent);
            return couponEvent;
        }
    }

    /**
     * ✅ ReentrantLock을 활용한 쿠폰 발급 (동시성 제어)
     */
    @Override
    public void issueCoupon(Long couponEventId) {
        Lock lock = locks.computeIfAbsent(couponEventId, k -> new ReentrantLock());
        lock.lock();

        try {
            CouponEvent event = store.get(couponEventId);
            if (event == null) {
                throw new CouponEventNotFoundException(CouponErrorCode.COUPON_EVENT_NOT_FOUND);
            }

            // Entity의 비즈니스 로직 호출 (검증 및 수량 증가)
            event.issue();

        } finally {
            lock.unlock();
        }
    }
}
