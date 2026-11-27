package pl.wishka.auth.application;

import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wishka.user.api.NewUser;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;

@Service
public class RegistrationService {

    private final UserAccounts userAccounts;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService verification;

    public RegistrationService(
            UserAccounts userAccounts, PasswordEncoder passwordEncoder, EmailVerificationService verification) {
        this.userAccounts = userAccounts;
        this.passwordEncoder = passwordEncoder;
        this.verification = verification;
    }

    /** Creates the account and queues the verification email; both commit together or not at all. */
    @Transactional
    public UserAccount register(String email, String password, String displayName, Locale locale) {
        UserAccount account = userAccounts.create(
                new NewUser(email, displayName, passwordEncoder.encode(password), locale, false, true));
        verification.sendVerification(account);
        return account;
    }
}
