package pl.wishka.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wishka.auth.application.AuthLimits;
import pl.wishka.auth.application.EmailRateLimiter;
import pl.wishka.auth.application.EmailVerificationService;
import pl.wishka.auth.application.PasswordResetService;
import pl.wishka.auth.application.RegistrationService;
import pl.wishka.auth.security.LoginSession;
import pl.wishka.auth.security.WishkaUserPrincipal;
import pl.wishka.auth.web.AuthRequests.LoginRequest;
import pl.wishka.auth.web.AuthRequests.PasswordResetConfirmRequest;
import pl.wishka.auth.web.AuthRequests.PasswordResetRequest;
import pl.wishka.auth.web.AuthRequests.RegisterRequest;
import pl.wishka.auth.web.AuthRequests.TokenRequest;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.error.ErrorCode;
import pl.wishka.common.i18n.SupportedLocales;
import pl.wishka.common.ratelimit.IpRateLimiter;
import pl.wishka.common.security.CurrentUserId;
import pl.wishka.common.web.ClientIp;
import pl.wishka.user.api.UserAccount;

/** Registration, password login, email verification and password reset. Logout is handled by Spring Security. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registration;
    private final EmailVerificationService verification;
    private final PasswordResetService passwordReset;
    private final AuthenticationManager authenticationManager;
    private final LoginSession loginSession;
    private final IpRateLimiter ipLimiter;
    private final EmailRateLimiter emailLimiter;

    public AuthController(
            RegistrationService registration,
            EmailVerificationService verification,
            PasswordResetService passwordReset,
            AuthenticationManager authenticationManager,
            LoginSession loginSession,
            IpRateLimiter ipLimiter,
            EmailRateLimiter emailLimiter) {
        this.registration = registration;
        this.verification = verification;
        this.passwordReset = passwordReset;
        this.authenticationManager = authenticationManager;
        this.loginSession = loginSession;
        this.ipLimiter = ipLimiter;
        this.emailLimiter = emailLimiter;
    }

    /** Makes sure the {@code XSRF-TOKEN} cookie exists; the SPA calls it on startup and after login/logout. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    /** Creates the account, sends the verification email and logs the new user in (unverified, D-6). */
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest body,
            Locale requestLocale,
            HttpServletRequest request,
            HttpServletResponse response) {
        ipLimiter.consume(AuthLimits.REGISTER_PER_IP, ClientIp.of(request));
        Locale locale = body.locale() != null ? Locale.of(body.locale()) : SupportedLocales.normalize(requestLocale);
        UserAccount account = registration.register(body.email(), body.password(), body.displayName(), locale);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                new WishkaUserPrincipal(account.id(), null), null, WishkaUserPrincipal.authorities());
        loginSession.establish(authentication, request, response);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        ipLimiter.consume(AuthLimits.LOGIN_PER_IP, ClientIp.of(request));
        if (emailLimiter.isExhausted(AuthLimits.LOGIN_FAILURES_PER_EMAIL, body.email())) {
            throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.email(), body.password()));
        } catch (AuthenticationException e) {
            emailLimiter.increment(AuthLimits.LOGIN_FAILURES_PER_EMAIL, body.email());
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        emailLimiter.reset(AuthLimits.LOGIN_FAILURES_PER_EMAIL, body.email());
        loginSession.establish(authentication, request, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest body) {
        verification.verify(body.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<Void> resendVerification(@CurrentUserId long userId) {
        verification.resend(userId);
        return ResponseEntity.accepted().build();
    }

    /** Always 202, whether or not the account exists (no account enumeration). */
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest body, HttpServletRequest request) {
        ipLimiter.consume(AuthLimits.RESET_PER_IP, ClientIp.of(request));
        passwordReset.request(body.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest body) {
        passwordReset.confirm(body.token(), body.password());
        return ResponseEntity.noContent().build();
    }
}
