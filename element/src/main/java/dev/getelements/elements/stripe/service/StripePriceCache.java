package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import dev.getelements.elements.stripe.StripeApplication;
import dev.getelements.elements.stripe.model.PriceSummary;
import jakarta.inject.Named;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for Stripe price listings. Singleton so the cache is shared across all
 * requests. Entries expire after the configured TTL and are refreshed lazily on the next call.
 */
public class StripePriceCache {

    private record Entry(List<PriceSummary> prices, long fetchedAt) {
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - fetchedAt > ttlMs;
        }
    }

    private final long ttlMs;
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    @Inject
    StripePriceCache(@Named(StripeApplication.PRICE_CACHE_TTL_MS) String ttlMs) {
        this.ttlMs = Long.parseLong(ttlMs);
    }

    /** Returns the cached prices for the given key, or {@code null} if absent or expired. */
    public List<PriceSummary> get(String key) {
        final var entry = cache.get(key);
        if (entry == null || entry.isExpired(ttlMs)) {
            cache.remove(key);
            return null;
        }
        return entry.prices();
    }

    /** Stores prices under the given key, resetting the expiry timer. */
    public void put(String key, List<PriceSummary> prices) {
        cache.put(key, new Entry(prices, System.currentTimeMillis()));
    }

    /** Removes all cached entries, forcing a fresh fetch on the next call. */
    public void invalidate() {
        cache.clear();
    }

}
