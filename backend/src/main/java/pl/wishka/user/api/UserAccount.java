package pl.wishka.user.api;

import java.util.List;
import java.util.Locale;
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
        List<LinkedLogin> linkedLogins) {

    public boolean hasPassword() {
        return passwordHash != null;
    }

    /** Number of ways this user can log in; the last one can never be removed (D-38). */
    public int loginMethodCount() {
        return (hasPassword() ? 1 : 0) + linkedLogins.size();
    }

    public boolean hasLinked(AuthProvider provider) {
        return linkedLogins.stream().anyMatch(login -> login.provider() == provider);
    }
}
