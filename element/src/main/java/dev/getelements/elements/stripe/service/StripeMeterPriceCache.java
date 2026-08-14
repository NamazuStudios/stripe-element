package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import dev.getelements.elements.stripe.StripeApplication;
import dev.getelements.elements.stripe.model.PriceSummary;
import jakarta.inject.Named;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for {@link StripeService#resolvePriceForMeterEventName}, mirroring
 * {@link StripePriceCache}'s shape but keyed by meter event name. Singleton so the cache is shared
 * across all requests. Reuses {@link StripeApplication#PRICE_CACHE_TTL_MS} rather than a dedicated
 * config key — conceptually the same kind of cache, just keyed differently.
 */
public class StripeMeterPriceCache {

    private record Entry(Optional<PriceSummary> price, long fetchedAt) {
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - fetchedAt > ttlMs;
        }
    }

    private final long ttlMs;
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    @Inject
    StripeMeterPriceCache(@Named(StripeApplication.PRICE_CACHE_TTL_MS) String ttlMs) {
        this.ttlMs = Long.parseLong(ttlMs);
    }

    /** Returns the cached result for the given meter event name, or {@code null} if absent or expired. */
    public Optional<PriceSummary> get(String eventName) {
        final var entry = cache.get(eventName);
        if (entry == null || entry.isExpired(ttlMs)) {
            cache.remove(eventName);
            return null;
        }
        return entry.price();
    }

    /** Stores the resolved price (possibly empty) under the given event name, resetting the expiry timer. */
    public void put(String eventName, Optional<PriceSummary> price) {
        cache.put(eventName, new Entry(price, System.currentTimeMillis()));
    }

    /** Removes all cached entries, forcing a fresh fetch on the next call. */
    public void invalidate() {
        cache.clear();
    }

}
