package pl.wishka.auth.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wishka.common.config.AppProperties;

/** Tells the frontend which login methods are available (the Google button is hidden when it isn't configured). */
@RestController
public class AuthConfigController {

    private final AppProperties app;

    public AuthConfigController(AppProperties app) {
        this.app = app;
    }

    @GetMapping("/api/public/auth/config")
    public AuthConfigResponse config() {
        return new AuthConfigResponse(app.google().enabled());
    }

    public record AuthConfigResponse(boolean googleEnabled) {}
}
