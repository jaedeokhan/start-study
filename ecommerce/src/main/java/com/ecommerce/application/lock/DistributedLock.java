package com.ecommerce.application.lock;

import com.ecommerce.application.lock.constant.LockType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();
    LockType type() default LockType.PUB_SUB;
    long waitTime() default 3000L;
    long leaseTime() default 5000L;
}
