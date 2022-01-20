package com.nullpointerw.redisLock.executor;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * @Author: wkc
 * @Date: 2022/1/20 14:10
 */
public class RedisTemplateLockExecutor {
    public static final RedisScript<String> LUA_EVAL_SCRIPT_UNLOCK = new DefaultRedisScript<>("if redis.call('get',KEYS[1]) " +
            "== ARGV[1] then return tostring(redis.call('del', KEYS[1])) else return 'false' end", String.class);
}
