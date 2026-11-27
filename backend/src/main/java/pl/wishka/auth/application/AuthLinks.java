package pl.wishka.auth.application;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import pl.wishka.common.config.AppProperties;

/** Absolute links to frontend pages, always built from {@code APP_BASE_URL} (never from the request). */
@Component
public class AuthLinks {

    private final AppProperties app;

    public AuthLinks(AppProperties app) {
        this.app = app;
    }

    public String verifyEmail(String rawToken) {
        return link("/verify", rawToken);
    }

    public String resetPassword(String rawToken) {
        return link("/reset", rawToken);
    }

    private String link(String path, String rawToken) {
        return UriComponentsBuilder.fromUri(app.baseUrl())
                .path(path)
                .queryParam("token", rawToken)
                .build()
                .toUriString();
    }
}
