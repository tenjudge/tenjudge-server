package io.github.yush1x.tenjudge.server.infra;

import io.github.yush1x.tenjudge.server.config.AppCacheProperties;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final AppCacheProperties appCacheProperties;

    private static final String NULL_VALUE = "NULL_VALUE"; // 空值

    /**
     * 通用缓存读取入口。
     * 先读 Redis，未命中时再通过 loader 回源，并在回源阶段使用 Redisson 锁避免高并发下重复打数据库。
     *
     * @param key 缓存 key   "user:" + userId,
     * @param clazz 缓存值目标类型，用于从 Redis 结果转换为业务对象   UserDTO.class
     * @param ttlName 缓存 TTL 配置名，对应 app.cache-ttl 下的配置项
     * @param loader 缓存未命中时的数据加载逻辑 () -> userMapper.selectById(userId)
     * @param <T> 业务对象类型
     * @return 命中的缓存或回源后的结果；若数据源为空则返回 null
     */
    public <T> T get(String key, Class<T> clazz, String ttlName, Supplier<T> loader) {
        return get(key, clazz, appCacheProperties.getCacheTtl(ttlName), loader);
    }

    public <T> T get(String key, Class<T> clazz, Duration ttl, Supplier<T> loader) {
        Object cached = redisTemplate.opsForValue().get(key);

        // 1. 缓存存在
        if (cached != null) {
            // 空值缓存（缓存穿透）
            if (NULL_VALUE.equals(cached)) {
                return null;
            }
            return clazz.cast(cached); // 找到缓存
        }

        // 2. 缓存不存在

        RLock lock = redissonClient.getLock("lock:cache:" + key); // 加锁防止缓存击穿
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("RedisService 获取锁失败，无法将数据更新至缓存");
            }

            // 重新检查缓存，若此时已经有线程更新完数据，则直接返回缓存
            cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                if (NULL_VALUE.equals(cached)) {
                    return null;
                }
                return clazz.cast(cached);
            }

            // 如果缓存依然没有命中，执行数据加载逻辑
            T value = loader.get();

            // 防止缓存穿透，如果数据库返回空值，则缓存"null"标识，并设置短TTL
            if (value == null) {
                redisTemplate.opsForValue().set(key, NULL_VALUE, appCacheProperties.getCacheTtl("null-value"));
                return null;
            }

            // 将查询结果写入缓存，并设置过期时间
            redisTemplate.opsForValue().set(key, value, ttl);
            return value;


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("RedisService 缓存锁等待被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }


    }

    /**
     * 读取普通 Redis 字符串值
     */
    public <T> T getValue(String key, Class<T> clazz) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null || NULL_VALUE.equals(cached)) {
            return null;
        }
        return clazz.cast(cached);
    }

    /**
     * 写入普通 Redis 字符串值，TTL 名称统一从 app.cache-ttl 读取。
     */
    public void set(String key, Object value, String ttlName) {
        redisTemplate.opsForValue().set(key, value, appCacheProperties.getCacheTtl(ttlName));
    }

    /**
     * 删除指定缓存键，供数据库写入成功后的显式失效使用。
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
