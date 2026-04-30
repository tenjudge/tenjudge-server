package io.github.yush1x.tenjudge.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app")
public class AppCacheProperties {

    private static final Map<String, Duration> DEFAULT_CACHE_TTLS = Map.of(
            "user-role", Duration.ofHours(1),
            "problem", Duration.ofHours(5),
            "problem-tags", Duration.ofHours(5),
            "contest-problem", Duration.ofHours(5),
            "contest-detail", Duration.ofSeconds(60),
            "contest-list", Duration.ofSeconds(60),
            "null-value", Duration.ofSeconds(60),
            "spring-cache-default", Duration.ofHours(1)
    );

    private Map<String, Duration> cacheTtl = new HashMap<>();

    public Map<String, Duration> getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Map<String, Duration> cacheTtl) {
        this.cacheTtl = cacheTtl == null ? new HashMap<>() : cacheTtl;
    }

    public Duration getCacheTtl(String name) {
        Duration ttl = cacheTtl.get(name);
        if (ttl != null) {
            return ttl;
        }

        ttl = DEFAULT_CACHE_TTLS.get(name);
        if (ttl != null) {
            return ttl;
        }

        throw new RuntimeException("Redis 缓存 TTL 未配置: app.cache-ttl." + name);
    }
}
