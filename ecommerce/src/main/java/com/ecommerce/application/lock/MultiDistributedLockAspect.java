package com.ecommerce.application.lock;

import com.ecommerce.application.lock.constant.LockConstants;
import com.ecommerce.application.lock.exception.LockAcquisitionException;
import com.ecommerce.domain.common.exception.BaseException;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(0)  // @Transactional보다 먼저 실행
@RequiredArgsConstructor
@Slf4j
public class MultiDistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();
    private static final String LOCK_PREFIX = LockConstants.LOCK_PREFIX;

    @Around("@annotation(multiDistributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, MultiDistributedLock multiDistributedLock)
            throws Throwable {

        List<String> lockKeys = resolveLockKeys(multiDistributedLock, joinPoint);

        if (lockKeys == null || lockKeys.isEmpty()) {
            log.warn("락 키가 없습니다. 락 없이 실행합니다.");
            return joinPoint.proceed();
        }

        // 데드락 방지를 위해 정렬
        lockKeys.sort(String::compareTo);

        log.debug("MultiLock 획득 시도: {}", lockKeys);

        // MultiLock 생성 및 실행
        return executeMultiLock(joinPoint, lockKeys, multiDistributedLock);
    }

    /**
     * LockKeyProvider를 통한 락 키 목록 해석
     */
    private List<String> resolveLockKeys(
            MultiDistributedLock lockConfig,
            ProceedingJoinPoint joinPoint
    ) {
        StandardEvaluationContext context = createEvaluationContext(joinPoint);
        Expression exp = parser.parseExpression(lockConfig.keyProvider());
        Object value = exp.getValue(context);

        // List<String> 직접 반환
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> keys = (List<String>) value;
            return keys;
        }

        throw new IllegalArgumentException("keyProvider는 List<String>을 반환해야 합니다: " + lockConfig.keyProvider());
    }

    /**
     * SpEL 평가 컨텍스트 생성
     */
    private StandardEvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());

        // 메서드 파라미터를 컨텍스트에 추가
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        return context;
    }

    /**
     * MultiLock 획득 및 비즈니스 로직 실행
     */
    private Object executeMultiLock(
            ProceedingJoinPoint joinPoint,
            List<String> lockKeys,
            MultiDistributedLock config
    ) throws Throwable {
        // MultiLock 생성
        RLock[] locks = lockKeys.stream()
                .map(key -> redissonClient.getLock(LOCK_PREFIX + key))
                .toArray(RLock[]::new);

        RLock multiLock = redissonClient.getMultiLock(locks);

        try {
            // 모든 락 획득 시도
            boolean acquired = multiLock.tryLock(
                    config.waitTime(),
                    config.leaseTime(),
                    TimeUnit.MILLISECONDS
            );

            if (!acquired) {
                log.warn(
                    "[FAIL] Acquire MultiLock. keys={}, threadId={}",
                    lockKeys,
                    Thread.currentThread().getId()
                );
                throw new LockAcquisitionException("멀티락 획득 시 타임아웃 실패 : " + lockKeys);
            }

            log.info(
                    "[SUCCESS] Acquired lock. keys={}, threadId={}",
                    lockKeys,
                    Thread.currentThread().getId()
            );

            return joinPoint.proceed();

        } catch (BaseException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(
                    "[FAIL] MultiLock acquisition interrupted keys={}, threadId={}",
                    lockKeys,
                    Thread.currentThread().getId(),
                    e
            );
            throw new LockAcquisitionException("락 획득 시 인터럽트 발생 : " + lockKeys, e);
        } finally {
            // 락 해제
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }
}