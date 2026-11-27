package pl.wishka.common.ratelimit;

import java.time.Duration;

/** A limit of {@code max} attempts per {@code window}, e.g. 30 login attempts per 15 minutes. */
public record RateLimit(String name, int max, Duration window) {}
