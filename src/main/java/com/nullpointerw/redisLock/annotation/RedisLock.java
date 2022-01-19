package com.nullpointerw.redisLock.annotation;


import com.nullpointerw.redisLock.enums.LockPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wkc
 * @Date: 2022/1/17 13:39
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {
    /**
     * 锁key
     *
     * @return
     */
    String key();

    /**
     * 锁有效时间
     * @return
     */
    long expire() default 5L;

    /**
     * 时间单位
     * @return
     */

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * key参数位置
     * @return
     */
    int arg() default 0;

    /**
     * 获取锁策略
     * @return
     */
    LockPolicy lockPolicy() default LockPolicy.LOOP;


    boolean argRequire() default true;

}


