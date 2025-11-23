package com.ecommerce.infrastructure.repository;

import com.ecommerce.domain.coupon.CouponEvent;
import com.ecommerce.domain.coupon.exception.CouponErrorCode;
import com.ecommerce.domain.coupon.exception.CouponEventNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponEventRepository extends JpaRepository<CouponEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponEvent c WHERE c.id = :id")
    Optional<CouponEvent> findByIdWithLock(@Param("id") Long id);

    default CouponEvent findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new CouponEventNotFoundException(CouponErrorCode.COUPON_EVENT_NOT_FOUND));
    }

    List<CouponEvent> findAll();

    CouponEvent save(CouponEvent couponEvent);

}
