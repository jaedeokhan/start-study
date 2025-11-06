package com.ecommerce.infrastructure.memory;

import com.ecommerce.domain.user.User;
import com.ecommerce.domain.user.exception.UserNotFoundException;
import com.ecommerce.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User InMemory Repository 구현체
 * - ConcurrentHashMap 기반 인메모리 저장소
 * - ReentrantLock을 활용한 잔액 충전/차감 동시성 제어
 */
@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // ✅ 사용자별 Lock 관리 (동시성 제어)
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            Long newId = idGenerator.getAndIncrement();
            User newUser = new User(newId, user.getName(), user.getBalance());
            store.put(newId, newUser);
            return newUser;
        } else {
            store.put(user.getId(), user);
            return user;
        }
    }

    /**
     * ✅ ReentrantLock을 활용한 잔액 충전 (동시성 제어)
     */
    @Override
    public void chargeBalance(Long userId, long amount) {
        Lock lock = locks.computeIfAbsent(userId, k -> new ReentrantLock());
        lock.lock();

        try {
            User user = store.get(userId);
            if (user == null) {
                throw new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId);
            }

            // Entity의 비즈니스 로직 호출
            user.charge(amount);

        } finally {
            lock.unlock();
        }
    }

    /**
     * ✅ ReentrantLock을 활용한 잔액 차감 (동시성 제어)
     */
    @Override
    public void deductBalance(Long userId, long amount) {
        Lock lock = locks.computeIfAbsent(userId, k -> new ReentrantLock());
        lock.lock();

        try {
            User user = store.get(userId);
            if (user == null) {
                throw new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId);
            }

            // Entity의 비즈니스 로직 호출 (검증 포함)
            user.deduct(amount);

        } finally {
            lock.unlock();
        }
    }
}
