package pl.wishka.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wishka.auth.domain.EmailVerificationToken;
import pl.wishka.auth.domain.EmailVerificationTokenRepository;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.error.ErrorCode;
import pl.wishka.common.security.TokenHasher;
import pl.wishka.notification.api.NotificationEvent;
import pl.wishka.notification.api.NotificationPublisher;
import pl.wishka.notification.api.NotificationType;
import pl.wishka.notification.api.Recipient;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;

/** Email verification: 24-hour single-use links (D-6: unverified accounts are limited until verified). */
@Service
@Transactional(readOnly = true)
public class EmailVerificationService {

    static final Duration TOKEN_VALIDITY = Duration.ofHours(24);

    private final EmailVerificationTokenRepository tokens;
    private final UserAccounts userAccounts;
    private final NotificationPublisher notifications;
    private final EmailRateLimiter emailLimiter;
    private final AuthLinks links;
    private final Clock clock;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokens,
            UserAccounts userAccounts,
            NotificationPublisher notifications,
            EmailRateLimiter emailLimiter,
            AuthLinks links,
            Clock clock) {
        this.tokens = tokens;
        this.userAccounts = userAccounts;
        this.notifications = notifications;
        this.emailLimiter = emailLimiter;
        this.links = links;
        this.clock = clock;
    }

    /** Issues a fresh link (older ones stop working) and queues the email in the same transaction. */
    @Transactional
    public void sendVerification(UserAccount account) {
        Instant now = clock.instant();
        tokens.invalidateAll(account.id(), now);
        TokenHasher.Token token = TokenHasher.generate();
        tokens.save(new EmailVerificationToken(account.id(), token.hash(), now, now.plus(TOKEN_VALIDITY)));
        notifications.publish(NotificationEvent.of(
                NotificationType.ACCOUNT_VERIFY_EMAIL,
                Recipient.user(account.id()),
                Map.of("actionUrl", links.verifyEmail(token.raw()))));
    }

    /** "Resend" from the banner: at most once a minute; a no-op for an already verified account. */
    @Transactional
    public void resend(long userId) {
        UserAccount account = userAccounts.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (account.emailVerified()) {
            return;
        }
        if (!emailLimiter.tryAcquire(AuthLimits.VERIFY_RESEND_PER_EMAIL, account.email())) {
            throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
        }
        sendVerification(account);
    }

    @Transactional
    public void verify(String rawToken) {
        Instant now = clock.instant();
        EmailVerificationToken token = tokens.findByTokenHash(TokenHasher.hash(rawToken))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID_OR_EXPIRED));
        token.markUsed(now);
        userAccounts.markEmailVerified(token.userId());
        tokens.invalidateAll(token.userId(), now);
    }
}
