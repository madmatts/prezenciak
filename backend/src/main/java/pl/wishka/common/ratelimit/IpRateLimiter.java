package pl.wishka.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.error.ErrorCode;

/**
 * Per-IP rate limits kept in memory (D-10): one backend instance, so no Redis. Limits reset on restart, which is
 * acceptable for abuse protection. Per-email limits that must survive restarts live in the database instead.
 */
@Component
public class IpRateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Consumes one attempt or throws {@code TOO_MANY_REQUESTS}. */
    public void consume(RateLimit limit, String ip) {
        if (!tryConsume(limit, ip)) {
            throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    public boolean tryConsume(RateLimit limit, String ip) {
        Bucket bucket = buckets.computeIfAbsent(limit.name() + "|" + ip, key -> newBucket(limit));
        return bucket.tryConsume(1);
    }

    /** Drops idle buckets so the map doesn't grow without bound. Windows are at most one hour long. */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    void evictAll() {
        buckets.clear();
    }

    private static Bucket newBucket(RateLimit limit) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit.max())
                        .refillIntervally(limit.max(), limit.window())
                        .build())
                .build();
    }
}
