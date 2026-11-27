package pl.wishka.user.api;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Data for creating an account.
 *
 * @param passwordHash already-encoded password, or {@code null} for a Google-only account
 * @param emailVerified {@code true} when the email was verified by Google
 * @param privacyAccepted whether the user accepted the privacy policy during registration
 */
public record NewUser(
        String email,
        String displayName,
        @Nullable String passwordHash,
        Locale locale,
        boolean emailVerified,
        boolean privacyAccepted) {}
