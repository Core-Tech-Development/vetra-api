package dev.vetra.api.shared.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class ReactiveCacheService {

    private static final Logger LOG = Logger.getLogger(ReactiveCacheService.class);
    private static final String KEY_PREFIX = "vetra:";

    private final ReactiveRedisDataSource redisDataSource;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private ReactiveValueCommands<String, String> valueCommands;
    private ReactiveKeyCommands<String> keyCommands;

    @Inject
    public ReactiveCacheService(ReactiveRedisDataSource redisDataSource,
                                 ObjectMapper objectMapper,
                                 MeterRegistry meterRegistry) {
        this.redisDataSource = redisDataSource;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class, String.class);
        this.keyCommands = redisDataSource.key(String.class);
    }

    /**
     * Get-or-load pattern: check cache first, call loader on miss, store result.
     * On any Redis failure, falls back to the loader transparently.
     */
    public <T> Uni<T> getOrLoad(String cacheKey, Duration ttl,
                                 TypeReference<T> typeRef,
                                 Uni<T> loader) {
        String fullKey = KEY_PREFIX + cacheKey;
        String prefix = extractPrefix(cacheKey);

        return valueCommands.get(fullKey)
                .onFailure().invoke(e -> {
                    LOG.warnf("Redis GET failed for key %s: %s", fullKey, e.getMessage());
                    meterRegistry.counter("vetra_cache_errors", "cache", prefix).increment();
                })
                .onFailure().recoverWithNull()
                .flatMap(cached -> {
                    if (cached != null) {
                        try {
                            T value = objectMapper.readValue(cached, typeRef);
                            LOG.debugf("Cache HIT: %s", fullKey);
                            meterRegistry.counter("vetra_cache_hits", "cache", prefix).increment();
                            return Uni.createFrom().item(value);
                        } catch (JsonProcessingException e) {
                            LOG.warnf("Cache deserialization failed for key %s: %s", fullKey, e.getMessage());
                        }
                    }
                    LOG.debugf("Cache MISS: %s", fullKey);
                    meterRegistry.counter("vetra_cache_misses", "cache", prefix).increment();
                    return loader.flatMap(result -> put(fullKey, result, ttl)
                            .onFailure().recoverWithNull()
                            .replaceWith(result));
                });
    }

    /**
     * Explicit put into cache.
     */
    public <T> Uni<Void> put(String fullKey, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return valueCommands.setex(fullKey, ttl.toSeconds(), json);
        } catch (JsonProcessingException e) {
            LOG.warnf("Cache serialization failed for key %s: %s", fullKey, e.getMessage());
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * Invalidate a single key.
     */
    public Uni<Void> invalidate(String cacheKey) {
        String fullKey = KEY_PREFIX + cacheKey;
        return keyCommands.del(fullKey)
                .onFailure().recoverWithItem(0)
                .replaceWithVoid();
    }

    /**
     * Invalidate all keys matching a pattern (e.g., "pricing:exam-type:*").
     */
    public Uni<Void> invalidateByPattern(String pattern) {
        String fullPattern = KEY_PREFIX + pattern;
        return keyCommands.keys(fullPattern)
                .onFailure().recoverWithItem(List.of())
                .flatMap(keys -> {
                    if (keys.isEmpty()) return Uni.createFrom().voidItem();
                    return keyCommands.del(keys.toArray(new String[0]))
                            .replaceWithVoid();
                });
    }

    private String extractPrefix(String cacheKey) {
        int idx = cacheKey.indexOf(':');
        return idx > 0 ? cacheKey.substring(0, idx) : cacheKey;
    }
}
