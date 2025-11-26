package com.ecommerce.application.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiDistributedLock {
    /**
     * LockKeyProvider의 SpEL 표현식
     * 예: "getLockKeys(#userId)" 또는 "#lockKeyProvider"
     */
    String keyProvider();
    LockType type() default LockType.PUB_SUB;
    long waitTime() default 3000L;
    long leaseTime() default 5000L;
}
