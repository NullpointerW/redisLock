# redisLock
基于RedisTemplate的分布式锁
支持可重复入，多种获取策略，方便、快捷、易用，只需一个注解；可对同一方法不同参数定制灵活的颗粒度锁。
## 使用 
1、引入相关依赖
```xml

<dependency>
    <!--引入依赖-->
    <groupId>org.NullPointerW</groupId>
    <artifactId>redisLock-spring-boot-starter</artifactId>
    <version>0.0.1-RELEASE</version>
</dependency>

``` 
2、配置redis 
```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password:
    timeout: 7200
    ...
``` 
3、在方法上使用@RedisLock注解