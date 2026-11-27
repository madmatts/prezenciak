package pl.wishka.auth.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;

/** Loads a user for a password login by email. Accounts without a password (Google-only) can't log in this way. */
@Component
public class WishkaUserDetailsService implements UserDetailsService {

    private final UserAccounts userAccounts;

    public WishkaUserDetailsService(UserAccounts userAccounts) {
        this.userAccounts = userAccounts;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        UserAccount account = userAccounts
                .findByEmail(email)
                .filter(UserAccount::hasPassword)
                .orElseThrow(() -> new UsernameNotFoundException("No password login for this email"));
        return new WishkaUserPrincipal(account.id(), account.passwordHash());
    }
}
