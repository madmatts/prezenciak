package pl.wishka.common.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Phase 0 security baseline: public endpoints are open, everything else needs a session (login arrives in Phase 1).
 *
 * <p>CSRF uses the SPA setup: the token is sent in the readable {@code XSRF-TOKEN} cookie, and the frontend echoes it
 * in the {@code X-XSRF-TOKEN} header. 401/403 responses are delegated to {@code GlobalExceptionHandler}, so they are
 * localized Problem Details like every other error.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {"/api/public/**", "/actuator/health/**", "/v3/api-docs/**", "/error"};

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver)
            throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATHS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .csrf(csrf -> csrf.spa())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex))
                        .accessDeniedHandler((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex)));
        return http.build();
    }
}
