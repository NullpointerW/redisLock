package com.nullpointerw.redisLock.executor;

import com.nullpointerw.redisLock.aop.RedisLockAspect;
import com.nullpointerw.redisLock.exception.RedisLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wkc
 * @Date: 2022/1/20 14:10
 */
@Component
public class RedisTemplateLockExecutor {

    public static final RedisScript<String> LUA_EVAL_SCRIPT_IS_OWNER = new DefaultRedisScript<>("if" +
            " redis.call('get',KEYS[1]) " +
            "== ARGV[1] then return '1' else return '0' end", String.class);

    public static final RedisScript<String> LUA_EVAL_SCRIPT_UNLOCK = new DefaultRedisScript<>("if " +
            "redis.call('get',KEYS[1]) " +
            "== ARGV[1] then return tostring(redis.call('del', KEYS[1])) else return '0' end", String.class);

    private final Logger logger = LoggerFactory.getLogger(RedisTemplateLockExecutor.class);
    private final String OK = "1";
    private final RedisTemplate redisTemplate;

    @Autowired
    public RedisTemplateLockExecutor(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public boolean isOwner(String key, String val) {
        return Boolean.TRUE.equals(redisTemplate.execute((RedisCallback<Boolean>) conn -> {
            byte[] eval = (byte[]) conn.eval(LUA_EVAL_SCRIPT_IS_OWNER.getScriptAsString().getBytes(StandardCharsets.UTF_8)
                    , ReturnType.VALUE
                    , 1
                    , key.getBytes(StandardCharsets.UTF_8)
                    , val.getBytes(StandardCharsets.UTF_8));
            return eval != null && OK.equals(new String(eval, StandardCharsets.UTF_8));
        }));

    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public boolean isOwn(String val, String key) {
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
    public boolean lock(String key, long duration, TimeUnit timeUnit) throws RedisLockException {
        try {
            return Boolean.TRUE.equals(redisTemplate.execute(new RedisCallback<Boolean>() {
                @Override
                public Boolean doInRedis(RedisConnection redisConnection) throws DataAccessException {
                    try {
                        String val = String.valueOf(Thread.currentThread().getId());
                        long expire = timeUnit.toSeconds(duration);
                        byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
                        byte[] byteVal = val.getBytes(StandardCharsets.UTF_8);
                        Object set = redisConnection.execute("set", byteKey,
                                byteVal,
                                "NX".getBytes(StandardCharsets.UTF_8),
                                "EX".getBytes(StandardCharsets.UTF_8),
                                String.valueOf(expire).getBytes(StandardCharsets.UTF_8));
                        return "OK".equals(set);
                    } catch (Exception ex) {
                        logger.error("访问redis失败", ex);
                        throw ex;
                    }
                }
            }));
        } catch (Exception ex) {
            throw new RedisLockException("获取锁时发生错误", ex);
        }
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public void unlock(String key) {
        redisTemplate.execute((RedisCallback<Void>) redisConnection -> {
            byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
            redisConnection.del(byteKey);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public boolean unlock(String key, String val) {
        byte[] eval = (byte[]) redisTemplate.execute((RedisCallback<Object>) conn -> {
            return conn.eval(LUA_EVAL_SCRIPT_UNLOCK.getScriptAsString().getBytes(StandardCharsets.UTF_8)
                    , ReturnType.VALUE
                    , 1
                    , key.getBytes(StandardCharsets.UTF_8)
                    , val.getBytes(StandardCharsets.UTF_8));
        });

        return eval != null && OK.equals(new String(eval, StandardCharsets.UTF_8));
    }

}


