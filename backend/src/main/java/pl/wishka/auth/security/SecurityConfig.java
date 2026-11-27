package pl.wishka.auth.security;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.UriComponentsBuilder;
import pl.wishka.auth.application.GoogleAccountLinker;
import pl.wishka.common.config.AppProperties;

/**
 * Session-based security (CLAUDE.md §4): HttpOnly + SameSite session cookie stored by Spring Session JDBC, CSRF
 * protection for the SPA (token in the readable {@code XSRF-TOKEN} cookie, echoed in {@code X-XSRF-TOKEN}), no tokens
 * in the browser's storage.
 *
 * <p>401/403 are delegated to {@code GlobalExceptionHandler}, so they are localized Problem Details like every other
 * error. Google login is wired only when the client is configured.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    public static final String SESSION_COOKIE = "WISHKA_SESSION";

    private static final String[] PUBLIC_PATHS = {
        "/api/public/**", "/actuator/health/**", "/v3/api-docs/**", "/error", "/oauth2/**", "/login/oauth2/**"
    };
    private static final String[] PUBLIC_AUTH_POSTS = {
        "/api/auth/register", "/api/auth/login", "/api/auth/verify-email", "/api/auth/password-reset/**"
    };

    /**
     * The session cookie: HttpOnly, SameSite=Lax, {@code Secure} whenever the request is HTTPS (behind nginx in prod,
     * thanks to forwarded headers).
     */
    @Bean
    CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(SESSION_COOKIE);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setCookiePath("/");
        return serializer;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax"));
        return repository;
    }

    /** Applied on every login: new session id (fixation protection) and a fresh CSRF token. */
    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrfTokenRepository)));
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            ObjectProvider<ClientRegistrationRepository> googleClient,
            GoogleAccountLinker googleAccountLinker,
            AppProperties app)
            throws Exception {
        http.securityContext(context -> context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATHS)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_POSTS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
                .sessionManagement(session -> session.sessionAuthenticationStrategy(sessionAuthenticationStrategy))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies(SESSION_COOKIE))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex))
                        .accessDeniedHandler((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex)));

        if (googleClient.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth.userInfoEndpoint(
                            userInfo -> userInfo.oidcUserService(new GoogleOidcUserService(googleAccountLinker)))
                    .successHandler((request, response, authentication) ->
                            response.sendRedirect(frontendUrl(app, "/auth/complete", null)))
                    .failureHandler((request, response, exception) -> {
                        String error = exception instanceof OAuth2AuthenticationException oauthError
                                ? oauthError.getError().getErrorCode()
                                : "OAUTH_FAILED";
                        response.sendRedirect(frontendUrl(app, "/login", error));
                    }));
        }
        return http.build();
    }

    private static String frontendUrl(AppProperties app, String path, String oauthError) {
        var builder = UriComponentsBuilder.fromUri(app.baseUrl()).path(path);
        if (oauthError != null) {
            builder.queryParam("oauthError", oauthError);
        }
        return builder.build().toUriString();
    }
}
