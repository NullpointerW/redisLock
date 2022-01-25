package com.nullpointerw.redisLock.autoConfig;


import com.nullpointerw.redisLock.aop.RedisLockAspect;
import lombok.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: wkc
 * @Date: 2022/1/21 14:42
 */
@Data
@Configuration
@ConfigurationProperties(prefix ="redis-lock")

public class redisLockConfig implements InitializingBean {
    Long expireTime;
    Long acquireTimeOut;
    Boolean acquireLogTracking;
    private final Logger logger = LoggerFactory.getLogger(redisLockConfig.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        if(expireTime!=null||acquireTimeOut!=null||acquireLogTracking!=null){

            logger.info("REDIS LOCK CONFIG INFO:配置信息已读取");

            StringBuilder sb = new StringBuilder();
            sb.append("全局锁过期时间 : ");
            if(expireTime==null){
                sb.append("未配置");
            }else {
                sb.append(expireTime);
            }
            logger.info("REDIS LOCK CONFIG INFO:"+sb.toString());
            sb.setLength(0);
            sb.append("全局锁过期时间 : ");
            if(expireTime==null){
                sb.append("未配置");
            }else {
                sb.append(expireTime);
            }
            logger.info("REDIS LOCK CONFIG INFO:"+sb.toString());
            sb.setLength(0);
            sb.append("获取锁超时时间 : ");
            if(acquireTimeOut==null){
                sb.append("未配置");
            }else {
                sb.append(acquireTimeOut);
            }
            logger.info("REDIS LOCK CONFIG INFO:"+sb.toString());
            sb.setLength(0);
            if(Boolean.TRUE.equals(acquireLogTracking)){
                sb.append("锁获取日志追踪 : 开启");
            }
            else {
                sb.append("锁获取日志追踪 : 关闭");
            }
            logger.info("REDIS LOCK CONFIG INFO:"+sb.toString());
        }





    }
}
