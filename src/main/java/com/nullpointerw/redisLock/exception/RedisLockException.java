package com.nullpointerw.redisLock.exception;

/**
 * @Author: wkc
 * @Date: 2022/1/17 17:21
 */
public class RedisLockException extends  Exception{
       public RedisLockException(String msg){
           super(msg);
       }
}
