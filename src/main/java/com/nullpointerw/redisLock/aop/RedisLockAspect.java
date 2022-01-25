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
                throw new RedisLockException("获取方法参数失败:无参数或指定参数类型不是String!", e);
            }

        } else {
            fullName = PREFIX + lockKey;
        }
        //如果启用全局配置项，覆盖注解设置
        long expire = cfg.getExpireTime() != null ? cfg.getExpireTime() : annotation.expire();
        TimeUnit timeUnit = annotation.timeUnit();
        LockPolicy policy = annotation.lockPolicy();
        switch (policy) {
            case ONCE:
                if (lockExecutor.isOwner(getThreadId(), fullName)) {
                    return point.proceed();
                }
                logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE);
                if (lockExecutor.lock(fullName, getThreadId(),expire, timeUnit)) {
                    try {
                        logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE_SUCCESS);
                        return point.proceed();
                    } finally {
                        if (lockExecutor.unlock(fullName, String.valueOf(Thread.currentThread().getId()))) {
                            logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.UNLOCK_SUCCESS);
                        } else {
                            logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.UNLOCK_FAILURE);
                        }
                    }
                } else {
                    throw new RedisLockException("REDIS LOCK: thread-" + Thread.currentThread().getId() + " 未能获取锁,获取策略:[ONCE]");
                }
            default:
                try {
                    if (lockExecutor.isOwner(getThreadId(), fullName)) {
                        return point.proceed();
                    }
                    logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE);
                    if(cfg.getAcquireTimeOut()!=null&&cfg.getAcquireTimeOut()>0L){

                    }
                    while (!lockExecutor.lock(fullName, getThreadId(),expire, timeUnit)) {
                        logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE_FAILURE);
                    }
                    ;
                    logging(fullName, String.valueOf(Thread.currentThread().getId()), null, AcquireLoggingStatus.ACQUIRE_SUCCESS);
                    return point.proceed();
                } finally {
                    if (lockExecutor.unlock(fullName, String.valueOf(Thread.currentThread().getId()))) {
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
                    logger.info("REDIS LOCK: thread-{} 开始获取锁 lock key= {}", threadId, key);
                    break;
                case ACQUIRE_SUCCESS:
                    logger.warn("REDIS LOCK: thread-{} 获取锁成功 lock key= {}", threadId, key);
                    break;
                case ACQUIRE_FAILURE:
                    logger.warn("REDIS LOCK: thread-{} 获取锁失败,准备下一轮尝试 lock key= {}", threadId, key);
                    break;
                case UNLOCK_SUCCESS:
                    logger.warn("REDIS LOCK: thread-{} 释放锁成功 lock key= {}", threadId, key);
                    break;
                default:
                    logger.error("REDIS LOCK: thread-{} 释放锁失败 lock key= {}", threadId, key);
                    break;
            }
        }

    }

}
