package top.rymc.phira.main.network.request;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import top.rymc.phira.main.function.throwable.ThrowableFunction;

import java.util.concurrent.TimeUnit;

public class GenericCache<K, V> {
    private final Cache<K, V> cache;

    private GenericCache(long expireAfterWrite, TimeUnit unit, long maximumSize) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(expireAfterWrite, unit)
                .maximumSize(maximumSize)
                .recordStats()
                .build();
    }

    public static <K, V> GenericCache<K, V> create(long expireAfterWrite, TimeUnit unit, long maximumSize) {
        return new GenericCache<>(expireAfterWrite, unit, maximumSize);
    }

    public <E extends Exception> V get(K key, ThrowableFunction<K, V, E> loader) throws E {
        V value = cache.getIfPresent(key);
        if (value != null) {
            return value;
        }
        V loaded = loader.apply(key);
        if (loaded != null) {
            cache.put(key, loaded);
        }
        return loaded;
    }

    public void invalidate(K key) {
        cache.invalidate(key);
    }

    public void invalidateAll(Iterable<K> keys) {
        cache.invalidateAll(keys);
    }

    public void clear() {
        cache.invalidateAll();
    }

    public long size() {
        return cache.estimatedSize();
    }

    public String stats() {
        return cache.stats().toString();
    }
}
