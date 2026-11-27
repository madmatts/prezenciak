package pl.wishka.user.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.error.ErrorCode;
import pl.wishka.common.i18n.SupportedLocales;
import pl.wishka.user.api.AuthProvider;
import pl.wishka.user.api.LinkedLogin;
import pl.wishka.user.api.NewUser;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;
import pl.wishka.user.domain.User;
import pl.wishka.user.domain.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserAccountService implements UserAccounts {

    private final UserRepository users;
    private final Clock clock;

    public UserAccountService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return users.findById(userId).map(UserAccountService::toAccount);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return users.findByEmail(normalizeEmail(email)).map(UserAccountService::toAccount);
    }

    @Override
    public Optional<UserAccount> findByProvider(AuthProvider provider, String subject) {
        return users.findByProvider(provider, subject).map(UserAccountService::toAccount);
    }

    @Override
    @Transactional
    public UserAccount create(NewUser newUser) {
        String email = normalizeEmail(newUser.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        Instant now = clock.instant();
        User user = new User(
                email,
                newUser.displayName().strip(),
                newUser.passwordHash(),
                SupportedLocales.normalize(newUser.locale()),
                now);
        if (newUser.emailVerified()) {
            user.markEmailVerified(now);
        }
        if (newUser.privacyAccepted()) {
            user.acceptPrivacyPolicy(now);
        }
        try {
            return toAccount(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // Concurrent registration with the same email: uk_users_email.
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
    }

    @Override
    @Transactional
    public void changePassword(long userId, String passwordHash) {
        load(userId).changePassword(passwordHash, clock.instant());
    }

    @Override
    @Transactional
    public void removePassword(long userId) {
        User user = load(userId);
        if (user.passwordHash() == null) {
            return;
        }
        requireAnotherLoginMethod(user);
        user.removePassword(clock.instant());
    }

    @Override
    @Transactional
    public void markEmailVerified(long userId) {
        load(userId).markEmailVerified(clock.instant());
    }

    @Override
    @Transactional
    public UserAccount linkProvider(long userId, AuthProvider provider, String subject, String providerEmail) {
        Optional<User> owner = users.findByProvider(provider, subject);
        if (owner.isPresent() && !owner.get().id().equals(userId)) {
            throw new ApiException(ErrorCode.PROVIDER_ALREADY_LINKED);
        }
        User user = load(userId);
        if (user.providerLink(provider).isEmpty()) {
            user.linkProvider(provider, subject, providerEmail, clock.instant());
        }
        return toAccount(users.saveAndFlush(user));
    }

    @Override
    @Transactional
    public void unlinkProvider(long userId, AuthProvider provider) {
        User user = load(userId);
        if (user.providerLink(provider).isEmpty()) {
            return;
        }
        requireAnotherLoginMethod(user);
        user.unlinkProvider(provider, clock.instant());
    }

    @Override
    @Transactional
    public UserAccount updateProfile(long userId, @Nullable String displayName, @Nullable Locale locale) {
        User user = load(userId);
        Instant now = clock.instant();
        if (displayName != null) {
            user.rename(displayName.strip(), now);
        }
        if (locale != null) {
            user.changeLocale(SupportedLocales.normalize(locale), now);
        }
        return toAccount(users.saveAndFlush(user));
    }

    private User load(long userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }

    private static void requireAnotherLoginMethod(User user) {
        if (user.loginMethodCount() <= 1) {
            throw new ApiException(ErrorCode.LAST_LOGIN_METHOD, Map.of());
        }
    }

    static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static UserAccount toAccount(User user) {
        return new UserAccount(
                user.id(),
                user.email(),
                user.displayName(),
                user.passwordHash(),
                user.locale(),
                user.emailVerified(),
                user.providers().stream()
                        .map(link -> new LinkedLogin(link.provider(), link.email()))
                        .sorted(Comparator.comparing(LinkedLogin::provider))
                        .toList());
    }
}
