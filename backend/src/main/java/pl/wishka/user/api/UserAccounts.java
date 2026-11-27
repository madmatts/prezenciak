package pl.wishka.user.api;

import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The user module's API for other modules (auth, notifications). Other modules never touch the user tables directly.
 */
public interface UserAccounts {

    Optional<UserAccount> findById(long userId);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByProvider(AuthProvider provider, String subject);

    /** Creates a user. Throws {@code EMAIL_TAKEN} if the email is already registered. */
    UserAccount create(NewUser newUser);

    void changePassword(long userId, String passwordHash);

    /** Removes the password. Throws {@code LAST_LOGIN_METHOD} if it is the only way to log in. */
    void removePassword(long userId);

    void markEmailVerified(long userId);

    /** Links an external login. Throws {@code PROVIDER_ALREADY_LINKED} if it belongs to another user. */
    UserAccount linkProvider(long userId, AuthProvider provider, String subject, String providerEmail);

    /** Unlinks an external login. Throws {@code LAST_LOGIN_METHOD} if it is the only way to log in. */
    void unlinkProvider(long userId, AuthProvider provider);

    UserAccount updateProfile(long userId, @Nullable String displayName, @Nullable Locale locale);
}
