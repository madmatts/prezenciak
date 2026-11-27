package pl.wishka.auth.application;

import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wishka.auth.security.SessionTerminator;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.error.ErrorCode;
import pl.wishka.user.api.AuthProvider;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;

/** Sign-in methods on the profile page (D-38): set/change/remove the password, unlink Google. */
@Service
public class AccountSecurityService {

    private final UserAccounts userAccounts;
    private final PasswordEncoder passwordEncoder;
    private final SessionTerminator sessions;

    public AccountSecurityService(
            UserAccounts userAccounts, PasswordEncoder passwordEncoder, SessionTerminator sessions) {
        this.userAccounts = userAccounts;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
    }

    /**
     * Sets or changes the password. Changing requires the current one; a Google-only user sets a first password
     * without it. Other sessions are logged out, the current one stays.
     */
    @Transactional
    public void changePassword(
            long userId, @Nullable String currentPassword, String newPassword, @Nullable String currentSessionId) {
        UserAccount account = userAccounts.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (account.hasPassword()
                && (currentPassword == null || !passwordEncoder.matches(currentPassword, account.passwordHash()))) {
            throw new ApiException(ErrorCode.CURRENT_PASSWORD_INVALID);
        }
        userAccounts.changePassword(userId, passwordEncoder.encode(newPassword));
        sessions.terminateAllExcept(userId, currentSessionId);
    }

    @Transactional
    public void removePassword(long userId) {
        userAccounts.removePassword(userId);
    }

    @Transactional
    public void unlinkGoogle(long userId) {
        userAccounts.unlinkProvider(userId, AuthProvider.GOOGLE);
    }
}
