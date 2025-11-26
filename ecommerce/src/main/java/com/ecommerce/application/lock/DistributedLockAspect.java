package com.ecommerce.application.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "ecommerce:lock:";

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        String lockKey = resolveLockKey(distributedLock.key(), joinPoint);

        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    TimeUnit.MILLISECONDS
            );

            if (!acquired) {
                log.warn(
                    "[FAIL] Acquire lock. key={}, threadId={}",
                    lockKey,
                    Thread.currentThread().getId()
                );
                throw new LockAcquisitionException("락 획득 시 타임아웃 실패 : " + lockKey);
            }

            log.info(
                "[SUCCESS] Acquired lock. key={}, threadId={}",
                lockKey,
                Thread.currentThread().getId()
            );

            return joinPoint.proceed();
        } catch (Throwable e) {
            Thread.currentThread().interrupt();
            log.error(
                    "[FAIL] Lock acquisition interrupted key={}, threadId={}",
                    lockKey,
                    Thread.currentThread().getId(),
                    e
            );
            throw new LockAcquisitionException("락 획득 시 인터럽트 실패 : " + lockKey);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * SpEL 표현식을 평가하여 락 키 생성
     */
    private String resolveLockKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        // 1. 메서드 시그니처 정보 추출
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 2. 파라미터 이름 추출
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 3. SpEL 컨텍스트 생성
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 4. 파라미터를 컨텍스트에 등록
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        // 5. SpEL 표현식 파싱 및 평가
        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(keyExpression);
        Object value = expression.getValue(context);

        // 6. 락 키 반환
        return LOCK_PREFIX + (value != null ? value.toString() : "");
    }
}
