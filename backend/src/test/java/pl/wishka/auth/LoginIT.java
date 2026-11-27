package pl.wishka.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import pl.wishka.support.AbstractIntegrationTest;
import pl.wishka.support.TestClient;

class LoginIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "kot-w-butach-27";

    @Test
    void loginCreatesHttpOnlySameSiteSessionCookie() {
        String email = uniqueEmail();
        client().registered(email, PASSWORD);
        TestClient browser = client();

        var result = browser.login(email, PASSWORD);

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(browser.setCookieHeaders(result))
                .anySatisfy(cookie -> assertThat(cookie)
                        .startsWith("WISHKA_SESSION=")
                        .contains("HttpOnly")
                        .contains("SameSite=Lax"));
        assertThat(browser.get("/api/me")).hasStatus(HttpStatus.OK);
    }

    @Test
    void loginRotatesTheSessionId() {
        String email = uniqueEmail();
        client().registered(email, PASSWORD);
        TestClient browser = client();
        browser.get("/api/auth/csrf");
        browser.post("/api/auth/login", "{\"email\":\"nobody@example.com\",\"password\":\"whatever-123\"}");
        String before = browser.sessionCookie();

        browser.login(email, PASSWORD);

        assertThat(browser.sessionCookie()).isNotNull().isNotEqualTo(before);
    }

    @Test
    void wrongPasswordAndUnknownEmailLookTheSame() {
        String email = uniqueEmail();
        client().registered(email, PASSWORD);

        assertThat(client().login(email, "wrong-password-1"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("INVALID_CREDENTIALS"));
        assertThat(client().login(uniqueEmail(), "wrong-password-1"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("INVALID_CREDENTIALS"));
    }

    @Test
    void tooManyFailuresForOneEmailAreThrottled() {
        String email = uniqueEmail();
        client().registered(email, PASSWORD);
        for (int i = 0; i < 10; i++) {
            client().login(email, "wrong-password-" + i);
        }

        assertThat(client().login(email, PASSWORD))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("TOO_MANY_REQUESTS"));
    }

    @Test
    void logoutEndsTheSession() {
        String email = uniqueEmail();
        TestClient browser = client().registered(email, PASSWORD);

        assertThat(browser.post("/api/auth/logout")).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(browser.get("/api/me"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("UNAUTHORIZED"));
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() {
        String email = uniqueEmail();
        client().registered(email, PASSWORD);

        assertThat(client().postWithoutCsrf(
                                "/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("CSRF_INVALID"));
    }

    @Test
    void authConfigReportsGoogleDisabledWhenNotConfigured() {
        assertThat(client().get("/api/public/auth/config"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPathSatisfying("$.googleEnabled", g -> assertThat(g).isEqualTo(false));
    }
}
