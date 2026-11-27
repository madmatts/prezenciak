package pl.wishka.auth.security;

import java.io.Serial;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import pl.wishka.common.security.AuthenticatedUser;

/** Principal of a Google login. Its name is the Wishka user id, the same session index key as a password login. */
public class WishkaOidcUser extends DefaultOidcUser implements AuthenticatedUser {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long userId;

    public WishkaOidcUser(long userId, OidcIdToken idToken, OidcUserInfo userInfo) {
        super(WishkaUserPrincipal.USER_AUTHORITIES, idToken, userInfo);
        this.userId = userId;
    }

    @Override
    public long userId() {
        return userId;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
