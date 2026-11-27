package pl.wishka.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Turns an authenticated user into a logged-in session for the JSON login and registration endpoints: rotates the
 * session id (fixation protection), rotates the CSRF token and stores the security context in the session.
 */
@Component
public class LoginSession {

    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;

    public LoginSession(SecurityContextRepository contextRepository, SessionAuthenticationStrategy sessionStrategy) {
        this.contextRepository = contextRepository;
        this.sessionStrategy = sessionStrategy;
    }

    public void establish(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        sessionStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
