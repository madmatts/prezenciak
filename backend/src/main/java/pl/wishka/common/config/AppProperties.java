package pl.wishka.common.config;

import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Environment-specific application settings. The product name and domain are configuration, never literals in code
 * (see CLAUDE.md "Naming and branding").
 *
 * @param name product name shown to users ("Wishka"), never translated
 * @param baseUrl absolute URL used for every link we generate (emails, push, share links)
 * @param mailFrom sender address for outgoing email
 * @param imageStoragePath directory where downloaded item images are stored
 * @param google Google login client; login with Google is enabled only when both values are set
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String name,
        URI baseUrl,
        @NotBlank String mailFrom,
        @NotBlank String imageStoragePath,
        Google google) {

    public record Google(
            @Nullable String clientId, @Nullable String clientSecret) {

        public boolean enabled() {
            return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
