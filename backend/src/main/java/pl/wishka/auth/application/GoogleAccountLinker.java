package pl.wishka.auth.application;

import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.error.ErrorCode;
import pl.wishka.user.api.AuthProvider;
import pl.wishka.user.api.NewUser;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;

/**
 * Maps a Google identity to a Wishka user (D-5):
 *
 * <ol>
 *   <li>logged in already (profile "Connect Google") → link to the current user;
 *   <li>Google account linked before → that user;
 *   <li>same (Google-verified) email as an existing account → link and log in, Google alone is enough afterwards;
 *   <li>otherwise → new account, email already verified by Google, no password.
 * </ol>
 */
@Service
public class GoogleAccountLinker {

    private final UserAccounts userAccounts;

    public GoogleAccountLinker(UserAccounts userAccounts) {
        this.userAccounts = userAccounts;
    }

    public record GoogleIdentity(
            String subject,
            String email,
            boolean emailVerified,
            @Nullable String name) {}

    @Transactional
    public long resolveUserId(GoogleIdentity identity, @Nullable Long currentUserId, Locale locale) {
        if (!identity.emailVerified()) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (currentUserId != null) {
            return userAccounts
                    .linkProvider(currentUserId, AuthProvider.GOOGLE, identity.subject(), identity.email())
                    .id();
        }
        Optional<UserAccount> linked = userAccounts.findByProvider(AuthProvider.GOOGLE, identity.subject());
        if (linked.isPresent()) {
            return linked.get().id();
        }
        Optional<UserAccount> sameEmail = userAccounts.findByEmail(identity.email());
        if (sameEmail.isPresent()) {
            long userId = sameEmail.get().id();
            userAccounts.linkProvider(userId, AuthProvider.GOOGLE, identity.subject(), identity.email());
            userAccounts.markEmailVerified(userId);
            return userId;
        }
        UserAccount created =
                userAccounts.create(new NewUser(identity.email(), displayName(identity), null, locale, true, false));
        return userAccounts
                .linkProvider(created.id(), AuthProvider.GOOGLE, identity.subject(), identity.email())
                .id();
    }

    private static String displayName(GoogleIdentity identity) {
        if (identity.name() != null && !identity.name().isBlank()) {
            return identity.name().length() > 100 ? identity.name().substring(0, 100) : identity.name();
        }
        return identity.email().substring(0, identity.email().indexOf('@'));
    }
}
