package com.ecommerce.infrastructure.jpa;

import com.ecommerce.domain.coupon.CouponEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponEventJpaRepository extends JpaRepository<CouponEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponEvent c WHERE c.id = :id")
    Optional<CouponEvent> findByIdWithLock(@Param("id") Long id);
}
