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

默认使用
```java

@RedisLock(key = "order")
public boolean  placeOrder(String uid) {
        ....
        }
``` 

####可选参数
设置锁的过期时间
```java

@RedisLock(key = "order",expire = 5,timeUnit = TimeUnit.MINUTES)
public boolean  placeOrder(String uid) {
        ....
        }
```  

设置锁的二级名称的参数 
```java

@RedisLock(key = "order",expire = 5,timeUnit = TimeUnit.MINUTES,arg=1)
public boolean  placeOrder(String uid,String code) {
        ....
        }
```   
不设置二级名称
```java

@RedisLock(key = "order",expire = 5,timeUnit = TimeUnit.MINUTES,argRequire = false)
public boolean  placeOrder(String uid) {
        ....
        }
```

设置锁获取策略

只获取一次，失败则抛出异常 
```java

@RedisLock(key = "order",lockPolicy = LockPolicy.ONCE)
public boolean  placeOrder(String uid) {
        ....
        }
```    
循环尝试获取，直到获取成功
```java

@RedisLock(key = "order",lockPolicy = LockPolicy.LOOP)
public boolean  placeOrder(String uid) {
        ....
        }
```    
