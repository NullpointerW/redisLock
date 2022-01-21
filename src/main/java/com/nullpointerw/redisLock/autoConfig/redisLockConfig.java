package com.nullpointerw.redisLock.autoConfig;

import com.nullpointerw.redisLock.aop.RedisLockAspect;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.context.annotation.Configuration;

/**
 * @Author: wkc
 * @Date: 2022/1/21 14:42
 */
@Data
@Configuration
@ConditionalOnProperty(prefix = "redis-lock")
public class redisLockConfig implements InitializingBean {
    Long expireTime;
    Long acquireTimeOut;
    boolean acquireLogTracking;

    @Override
    public void afterPropertiesSet() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("REDIS LOCK :配置信息已读取 \n");
        sb.append("全局锁过期时间 : ");
        if(expireTime==null){
            sb.append("未配置");
        }else {
            sb.append(expireTime+"\n");
        }
        sb.append("获取锁超时时间 : ");
        if(acquireTimeOut==null){
            sb.append("未配置");
        }else {
            sb.append(acquireTimeOut+"\n");
        }
        if(acquireLogTracking){
            sb.append("锁获取日志追踪 : 开启");
        }
        else {
            sb.append("锁获取日志追踪 : 关闭 \n");
        }
       LoggerFactory.getLogger(redisLockConfig.class).info(sb.toString());
    }
}
