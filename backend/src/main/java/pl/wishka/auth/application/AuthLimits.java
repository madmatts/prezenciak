package pl.wishka.auth.application;

import java.time.Duration;
import pl.wishka.common.ratelimit.RateLimit;

/** Rate limits for account endpoints. Per-IP limits are in memory, per-email limits in the database (D-10). */
public final class AuthLimits {

    public static final RateLimit LOGIN_PER_IP = new RateLimit("login-ip", 30, Duration.ofMinutes(15));
    public static final RateLimit LOGIN_FAILURES_PER_EMAIL = new RateLimit("login", 10, Duration.ofMinutes(15));
    public static final RateLimit REGISTER_PER_IP = new RateLimit("register-ip", 10, Duration.ofHours(1));
    public static final RateLimit RESET_PER_IP = new RateLimit("reset-ip", 10, Duration.ofHours(1));
    public static final RateLimit RESET_PER_EMAIL = new RateLimit("password-reset", 3, Duration.ofHours(1));
    /** "Resend" is blocked for 60 s after each send (design: P0 banner). */
    public static final RateLimit VERIFY_RESEND_PER_EMAIL = new RateLimit("verify-resend", 1, Duration.ofSeconds(60));

    private AuthLimits() {}
}
