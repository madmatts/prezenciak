package pl.wishka.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wishka.auth.domain.PasswordResetToken;
import pl.wishka.auth.domain.PasswordResetTokenRepository;
import pl.wishka.auth.security.SessionTerminator;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.error.ErrorCode;
import pl.wishka.common.security.TokenHasher;
import pl.wishka.notification.api.NotificationEvent;
import pl.wishka.notification.api.NotificationPublisher;
import pl.wishka.notification.api.NotificationType;
import pl.wishka.notification.api.Recipient;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;

/**
 * "Forgot password": 1-hour single-use links. Requests never reveal whether an account exists. A Google-only user can
 * use this to set a first password (D-38).
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    static final Duration TOKEN_VALIDITY = Duration.ofHours(1);

    private final PasswordResetTokenRepository tokens;
    private final UserAccounts userAccounts;
    private final NotificationPublisher notifications;
    private final EmailRateLimiter emailLimiter;
    private final PasswordEncoder passwordEncoder;
    private final SessionTerminator sessions;
    private final AuthLinks links;
    private final Clock clock;

    public PasswordResetService(
            PasswordResetTokenRepository tokens,
            UserAccounts userAccounts,
            NotificationPublisher notifications,
            EmailRateLimiter emailLimiter,
            PasswordEncoder passwordEncoder,
            SessionTerminator sessions,
            AuthLinks links,
            Clock clock) {
        this.tokens = tokens;
        this.userAccounts = userAccounts;
        this.notifications = notifications;
        this.emailLimiter = emailLimiter;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.links = links;
        this.clock = clock;
    }

    /** Always succeeds from the caller's point of view (no account enumeration). */
    @Transactional
    public void request(String email) {
        Optional<UserAccount> account = userAccounts.findByEmail(email);
        if (account.isEmpty() || !emailLimiter.tryAcquire(AuthLimits.RESET_PER_EMAIL, email)) {
            return;
        }
        Instant now = clock.instant();
        long userId = account.get().id();
        tokens.invalidateAll(userId, now);
        TokenHasher.Token token = TokenHasher.generate();
        tokens.save(new PasswordResetToken(userId, token.hash(), now, now.plus(TOKEN_VALIDITY)));
        notifications.publish(NotificationEvent.of(
                NotificationType.ACCOUNT_PASSWORD_RESET,
                Recipient.user(userId),
                Map.of("actionUrl", links.resetPassword(token.raw()))));
    }

    /** The account a still-valid reset link belongs to (shown on the "set a new password" page). */
    public String emailForToken(String rawToken) {
        return usableToken(rawToken)
                .flatMap(token -> userAccounts.findById(token.userId()))
                .map(UserAccount::email)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED));
    }

    /** Sets the new password, verifies the email (the link proved ownership) and logs out every session. */
    @Transactional
    public void confirm(String rawToken, String newPassword) {
        Instant now = clock.instant();
        PasswordResetToken token =
                usableToken(rawToken).orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED));
        token.markUsed(now);
        tokens.invalidateAll(token.userId(), now);
        userAccounts.changePassword(token.userId(), passwordEncoder.encode(newPassword));
        userAccounts.markEmailVerified(token.userId());
        sessions.terminateAll(token.userId());
    }

    private Optional<PasswordResetToken> usableToken(String rawToken) {
        Instant now = clock.instant();
        return tokens.findByTokenHash(TokenHasher.hash(rawToken)).filter(t -> t.isUsable(now));
    }
}
