package com.nullpointerw.redisLock.aop;

import com.nullpointerw.redisLock.annotation.RedisLock;

import com.nullpointerw.redisLock.exception.RedisLockException;
import com.nullpointerw.redisLock.enums.LockPolicy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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

    @Autowired
    private RedisTemplate redisTemplate;


    @Pointcut("@annotation(com.nullpointerw.redisLock.annotation.RedisLock)")
    public void lockAspect() {
    }

    @Around("lockAspect()")
    public Object action(ProceedingJoinPoint point) throws Throwable {
        MethodSignature method = (MethodSignature) point.getSignature();
        RedisLock annotation = method.getMethod().getAnnotation(RedisLock.class);
        Object[] args = point.getArgs();
        String key = (String) args[annotation.arg()];
        String lockKey = annotation.key();
        //lock.{lockKey}.{arg}
        String fullName = PREFIX+ lockKey+"."+key;
        long expire = annotation.expire();
        TimeUnit timeUnit = annotation.timeUnit();
        LockPolicy policy = annotation.lockPolicy();
        switch (policy) {
            case ONCE:
                if (isOwn(String.valueOf(Thread.currentThread().getId()), fullName)) {
                    return point.proceed();
                }
                if (lock(fullName, expire, timeUnit)) {
                    try {
                        return point.proceed();
                    } finally {
                        unlock(fullName);
                    }
                } else {
                    throw new RedisLockException("REDIS KEY: thread:" + Thread.currentThread().getId() + "未能获取锁,获取策略:[ONCE]");
                }
            default:
                try {
                    if (isOwn(String.valueOf(Thread.currentThread().getId()), fullName)) {
                        return point.proceed();
                    }
                    while (!lock(fullName, expire, timeUnit)) {
                    }
                    return point.proceed();
                } finally {
                    unlock(fullName);
                }
        }

    }

    @SuppressWarnings("unchecked")
    private boolean isOwn(String val, String key) {
        return Boolean.TRUE.equals(redisTemplate.execute(new RedisCallback<Boolean>() {
            @Override
            public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {

                byte[] bytesVal = redisConnection.get(key.getBytes(StandardCharsets.UTF_8));
                if (bytesVal == null) {
                    return false;
                }
                String res = new String(bytesVal, StandardCharsets.UTF_8);
                return res.equals(val);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    private boolean lock(String key, long duration, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.execute(new RedisCallback<Boolean>() {
            @Override
            public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
                try {
                    String val = String.valueOf(Thread.currentThread().getId());
                    long expire = timeUnit.toSeconds(duration);
                    byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
                    byte[] byteVal = val.getBytes(StandardCharsets.UTF_8);
                    Boolean flag = redisConnection.setNX(byteKey, byteVal);
                    if (flag != null && flag) {
                        redisConnection.expire(byteKey, expire);
                    }
                    return flag;
                } catch (Exception e) {
                    logger.error("获取锁错误");
                }
                return false;
            }
        }));
    }

    @SuppressWarnings("unchecked")
    private void unlock(String key) {
        redisTemplate.execute((RedisCallback<Void>) redisConnection -> {
            byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
            redisConnection.del(byteKey);
            return null;
        });
    }


}
