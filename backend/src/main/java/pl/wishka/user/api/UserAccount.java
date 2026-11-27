package pl.wishka.user.api;

import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A user as seen by other modules (auth, notifications). {@code passwordHash} is only meant for the auth module's
 * login check.
 */
public record UserAccount(
        long id,
        String email,
        String displayName,
        @Nullable String passwordHash,
        Locale locale,
        boolean emailVerified,
        Set<AuthProvider> providers) {

    public boolean hasPassword() {
        return passwordHash != null;
    }

    /** Number of ways this user can log in; the last one can never be removed (D-38). */
    public int loginMethodCount() {
        return (hasPassword() ? 1 : 0) + providers.size();
    }
}
