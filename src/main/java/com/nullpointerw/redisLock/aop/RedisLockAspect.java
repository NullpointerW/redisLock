package com.nullpointerw.redisLock.aop;

import com.nullpointerw.redisLock.annotation.RedisLock;

import com.nullpointerw.redisLock.autoConfig.redisLockConfig;
import com.nullpointerw.redisLock.enums.AcquireLoggingStatus;
import com.nullpointerw.redisLock.exception.RedisLockException;
import com.nullpointerw.redisLock.enums.LockPolicy;
import com.nullpointerw.redisLock.executor.RedisTemplateLockExecutor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * @Author: wkc
 * @Date: 2022/1/17 13:43
 */
@Aspect
@Component

public class RedisLockAspect {

    private final Logger logger = LoggerFactory.getLogger(RedisLockAspect.class);
    final String PREFIX = "lock.";

    private final String LOCAL_MACHINE_ID = UUID.randomUUID().toString();
    private final ThreadLocal<String> threadId = new ThreadLocal<>();

    private final RedisTemplateLockExecutor lockExecutor;

    @Autowired
    public RedisLockAspect(RedisTemplateLockExecutor lockExecutor) {
        this.lockExecutor = lockExecutor;
    }

    @Autowired(required = false)
    redisLockConfig cfg;

    @Pointcut("@annotation(com.nullpointerw.redisLock.annotation.RedisLock)")
    public void lockAspect() {

    }

    @Around("lockAspect()")
    public Object action(ProceedingJoinPoint point) throws Throwable {
        MethodSignature method = (MethodSignature) point.getSignature();
        RedisLock annotation = method.getMethod().getAnnotation(RedisLock.class);
        String fullName = null;
        String lockKey = annotation.key();
        if (annotation.argRequire()) {
            try {
                Object[] args = point.getArgs();
                String key = (String) args[annotation.arg()];
                //lock.{lockKey}.{arg}
                fullName = PREFIX + lockKey + "." + key;
            } catch (Exception e) {
                throw new RedisLockException("????????????????????????:????????????????????????????????????String!", e);
            }

        } else {
            fullName = PREFIX + lockKey;
        }
        //????????????????????????????????????????????????
        long expire = cfg.getExpireTime() != null ? cfg.getExpireTime() : annotation.expire();
        TimeUnit timeUnit = annotation.timeUnit();
        LockPolicy policy = annotation.lockPolicy();
        switch (policy) {
            case ONCE:
                if (lockExecutor.isOwner(getThreadId(), fullName)) {
                    return point.proceed();
                }
                logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE);
                if (lockExecutor.lock(fullName, getThreadId(), expire, timeUnit)) {
                    try {
                        logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE_SUCCESS);
                        return point.proceed();
                    } finally {
                        if (lockExecutor.unlock(fullName, getThreadId())) {
                            logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.UNLOCK_SUCCESS);
                        } else {
                            logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.UNLOCK_FAILURE);
                        }
                    }
                } else {
                    throw new RedisLockException("REDIS LOCK: thread-" + Thread.currentThread().getId() + " ???????????????,????????????:[ONCE]");
                }
            default:
                long exp = 0;
                try {
                    if (lockExecutor.isOwner(getThreadId(), fullName)) {
                        return point.proceed();
                    }
                    logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE);
                    if (cfg.getAcquireTimeOut() != null && cfg.getAcquireTimeOut() > 0L) {
                        exp = System.currentTimeMillis() + cfg.getAcquireTimeOut();
                    }
                    while (!lockExecutor.lock(fullName, getThreadId(), expire, timeUnit)) {
                        logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE_FAILURE);
                        if (exp > 0 && System.currentTimeMillis() > exp) {
                            throw new RedisLockException("REDIS LOCK: thread-" + Thread.currentThread().getId() + " ???????????? time out:" + cfg.getAcquireTimeOut() + "ms");
                        }
                    }
                    ;
                    logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE_SUCCESS);
                    return point.proceed();
                } finally {
                    if (lockExecutor.unlock(fullName, getThreadId())) {
                        logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.UNLOCK_SUCCESS);
                    } else {
                        logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.UNLOCK_FAILURE);
                    }
                }
        }
    }

    private String getThreadId() {
        return LOCAL_MACHINE_ID + "|" + Thread.currentThread().getId();
    }

    private void logging(String key, String threadId, String msg, AcquireLoggingStatus status) {
        if (cfg != null && Boolean.TRUE.equals(cfg.getAcquireLogTracking())) {
            switch (status) {
                case ACQUIRE:
                    logger.info("REDIS LOCK: thread-{} ??????????????? lock key= {}", threadId, key);
                    break;
                case ACQUIRE_SUCCESS:
                    logger.warn("REDIS LOCK: thread-{} ??????????????? lock key= {}", threadId, key);
                    break;
                case ACQUIRE_FAILURE:
                    logger.warn("REDIS LOCK: thread-{} ???????????????,????????????????????? lock key= {}", threadId, key);
                    break;
                case UNLOCK_SUCCESS:
                    logger.warn("REDIS LOCK: thread-{} ??????????????? lock key= {}", threadId, key);
                    break;
                default:
                    logger.error("REDIS LOCK: thread-{} ??????????????? lock key= {}", threadId, key);
                    break;
            }
        }

    }

}
