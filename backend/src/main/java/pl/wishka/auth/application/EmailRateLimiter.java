package pl.wishka.auth.application;

import java.time.Duration;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.wishka.common.ratelimit.RateLimit;

/**
 * Per-email limits stored in the database, so they survive restarts (D-10). Fixed windows: the counter resets when
 * the window has passed. Runs in its own transaction, so a counted failure is kept even if the caller rolls back.
 */
@Component
public class EmailRateLimiter {

    private final JdbcTemplate jdbc;

    public EmailRateLimiter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Counts one attempt; returns {@code true} while the limit isn't exceeded. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(RateLimit limit, String email) {
        return increment(limit, email) <= limit.max();
    }

    /** Whether the limit is already reached, without counting an attempt (e.g. before checking a password). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isExhausted(RateLimit limit, String email) {
        Integer attempts =
                jdbc
                        .query("""
                        select attempts from email_rate_limits
                        where action = ? and email = ? and window_start > now() - make_interval(secs => ?)
                        """, (rs, row) -> rs.getInt(1), limit.name(), normalize(email), seconds(limit.window()))
                        .stream()
                        .findFirst()
                        .orElse(0);
        return attempts >= limit.max();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int increment(RateLimit limit, String email) {
        Integer attempts = jdbc.queryForObject(
                """
                insert into email_rate_limits (action, email, window_start, attempts)
                values (?, ?, now(), 1)
                on conflict (action, email) do update set
                    attempts = case when email_rate_limits.window_start <= now() - make_interval(secs => ?)
                                    then 1 else email_rate_limits.attempts + 1 end,
                    window_start = case when email_rate_limits.window_start <= now() - make_interval(secs => ?)
                                        then now() else email_rate_limits.window_start end
                returning attempts
                """, Integer.class, limit.name(), normalize(email), seconds(limit.window()), seconds(limit.window()));
        return attempts == null ? 0 : attempts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(RateLimit limit, String email) {
        jdbc.update("delete from email_rate_limits where action = ? and email = ?", limit.name(), normalize(email));
    }

    private static double seconds(Duration window) {
        return window.toSeconds();
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
