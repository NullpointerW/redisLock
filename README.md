# redisLock
基于RedisTemplate的分布式锁

使用原子指令/LUA脚本控制获取与释放锁过程中的原子性

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

## 可选参数 
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

### 设置获取锁策略 

只获取一次
```java

@RedisLock(key = "order",lockPolicy = LockPolicy.ONCE)
public boolean  placeOrder(String uid) {
        ....
        }
```     
获取失败后抛出异常 
```java

Caused by: com.nullpointerw.redisLock.exception.RedisLockException: REDIS KEY: thread:91未能获取锁,获取策略:[ONCE]
        at com.nullpointerw.redisLock.aop.RedisLockAspect.action(RedisLockAspect.java:79)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethodWithGivenArgs(AbstractAspectJAdvice.java:634)
        at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethod(AbstractAspectJAdvice.java:624)
        at org.springframework.aop.aspectj.AspectJAroundAdvice.invoke(AspectJAroundAdvice.java:72)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:175)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:750) 
        ...
```     
循环尝试获取，直到获取成功
```java

@RedisLock(key = "order",lockPolicy = LockPolicy.LOOP)
public boolean  placeOrder(String uid) {
        ....
        }
```    
