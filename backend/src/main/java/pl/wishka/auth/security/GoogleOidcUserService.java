package pl.wishka.auth.security;

import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import pl.wishka.auth.application.GoogleAccountLinker;
import pl.wishka.auth.application.GoogleAccountLinker.GoogleIdentity;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.i18n.SupportedLocales;
import pl.wishka.common.security.AuthenticatedUser;

/** Loads the Google user and maps it to a Wishka account through {@link GoogleAccountLinker}. */
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final GoogleAccountLinker linker;

    public GoogleOidcUserService(GoogleAccountLinker linker) {
        this.linker = linker;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser google = delegate.loadUser(request);
        GoogleIdentity identity = new GoogleIdentity(
                google.getSubject(),
                google.getEmail(),
                Boolean.TRUE.equals(google.getEmailVerified()),
                google.getFullName());
        Locale locale = SupportedLocales.normalize(LocaleContextHolder.getLocale());
        try {
            long userId = linker.resolveUserId(identity, currentUserId(), locale);
            return new WishkaOidcUser(userId, google.getIdToken(), google.getUserInfo());
        } catch (ApiException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error(e.code().name()), e);
        }
    }

    /** Set when an already logged-in user connects Google from the profile page. */
    private static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }
}
