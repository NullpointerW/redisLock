package com.nullpointerw.redisLock.aop;

import com.nullpointerw.redisLock.annotation.RedisLock;

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

    private final RedisTemplateLockExecutor lockExecutor;

    @Autowired
    public RedisLockAspect(RedisTemplateLockExecutor lockExecutor) {
        this.lockExecutor = lockExecutor;
    }

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

        long expire = annotation.expire();
        TimeUnit timeUnit = annotation.timeUnit();
        LockPolicy policy = annotation.lockPolicy();
        switch (policy) {
            case ONCE:
                if (lockExecutor.isOwner(String.valueOf(Thread.currentThread().getId()), fullName)) {
                    return point.proceed();
                }
                if (lockExecutor.lock(fullName, expire, timeUnit)) {
                    try {
                        return point.proceed();
                    } finally {
                        lockExecutor.unlock(fullName);
                    }
                } else {
                    throw new RedisLockException("REDIS KEY: thread:" + Thread.currentThread().getId() + "未能获取锁,获取策略:[ONCE]");
                }
            default:
                try {
                    if (lockExecutor.isOwner(String.valueOf(Thread.currentThread().getId()), fullName)) {
                        return point.proceed();
                    }
                    while (!lockExecutor.lock(fullName, expire, timeUnit)) ;
                    return point.proceed();
                } finally {
                    if (lockExecutor.unlock(fullName, String.valueOf(Thread.currentThread().getId()))) {
                        logger.info("REDIS KEY: thread:" + Thread.currentThread().getId() + "释放锁成功");
                    } else {
                        logger.error("REDIS KEY: thread:" + Thread.currentThread().getId() + "释放锁失败");
                    }
                }
        }
    }

}
