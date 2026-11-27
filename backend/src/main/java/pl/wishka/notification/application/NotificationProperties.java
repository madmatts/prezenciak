package pl.wishka.notification.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbox dispatcher settings ({@code app.notifications.*}).
 *
 * @param dispatchInterval pause between dispatcher runs
 * @param batchSize events locked and processed per transaction
 * @param maxAttempts attempts before an event is marked FAILED
 * @param initialBackoff delay after the first failure; doubles with every attempt
 * @param maxBackoff upper bound for the retry delay
 */
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        Duration dispatchInterval, int batchSize, int maxAttempts, Duration initialBackoff, Duration maxBackoff) {

    public Duration backoffAfter(int attempts) {
        long multiplier = 1L << Math.min(Math.max(attempts - 1, 0), 20);
        Duration delay = initialBackoff.multipliedBy(multiplier);
        return delay.compareTo(maxBackoff) > 0 ? maxBackoff : delay;
    }
}
