package pl.wishka.user.web;

import java.util.List;
import pl.wishka.user.api.AuthProvider;
import pl.wishka.user.api.UserAccount;

/**
 * The logged-in user's profile.
 *
 * @param providers linked external logins, e.g. {@code ["GOOGLE"]}
 * @param hasPassword whether a password is set (a Google-only user has none)
 */
public record MeResponse(
        long id,
        String email,
        String displayName,
        String locale,
        boolean emailVerified,
        boolean hasPassword,
        List<AuthProvider> providers) {

    static MeResponse from(UserAccount account) {
        return new MeResponse(
                account.id(),
                account.email(),
                account.displayName(),
                account.locale().getLanguage(),
                account.emailVerified(),
                account.hasPassword(),
                account.providers().stream().sorted().toList());
    }
}
