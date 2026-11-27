package pl.wishka.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;
import pl.wishka.common.config.AppProperties;

/**
 * Google login client, registered only when {@code GOOGLE_CLIENT_ID} and {@code GOOGLE_CLIENT_SECRET} are set. The
 * redirect URI is built from {@code APP_BASE_URL}, so it matches what's registered in the Google console.
 */
@Configuration(proxyBeanMethods = false)
@Conditional(GoogleLoginConfig.GoogleConfigured.class)
public class GoogleLoginConfig {

    public static final String REGISTRATION_ID = "google";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(AppProperties app) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE
                .getBuilder(REGISTRATION_ID)
                .clientId(app.google().clientId())
                .clientSecret(app.google().clientSecret())
                .redirectUri(app.baseUrl() + "/login/oauth2/code/" + REGISTRATION_ID)
                .scope("openid", "email", "profile")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    static class GoogleConfigured implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            var env = context.getEnvironment();
            return StringUtils.hasText(env.getProperty("app.google.client-id"))
                    && StringUtils.hasText(env.getProperty("app.google.client-secret"));
        }
    }
}
