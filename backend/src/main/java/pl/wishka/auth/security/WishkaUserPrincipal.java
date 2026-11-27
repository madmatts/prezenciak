package pl.wishka.auth.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pl.wishka.common.security.AuthenticatedUser;

/**
 * Principal of a password login, stored in the session (Spring Session JDBC serializes it). The username is the
 * user id, so sessions are indexed by a stable key that survives an email change.
 */
public final class WishkaUserPrincipal implements UserDetails, CredentialsContainer, AuthenticatedUser, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    static final List<GrantedAuthority> USER_AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    public static List<GrantedAuthority> authorities() {
        return USER_AUTHORITIES;
    }

    private final long userId;
    private @Nullable String passwordHash;

    public WishkaUserPrincipal(long userId, @Nullable String passwordHash) {
        this.userId = userId;
        this.passwordHash = passwordHash;
    }

    @Override
    public long userId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public @Nullable String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return USER_AUTHORITIES;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
