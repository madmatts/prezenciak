package pl.wishka.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import pl.wishka.auth.application.GoogleAccountLinker;
import pl.wishka.auth.application.GoogleAccountLinker.GoogleIdentity;
import pl.wishka.support.AbstractIntegrationTest;
import pl.wishka.support.TestClient;

class AccountSecurityIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "kot-w-butach-27";

    @Autowired
    GoogleAccountLinker linker;

    @Test
    void changingThePasswordRequiresTheCurrentOne() {
        String email = uniqueEmail();
        TestClient browser = client().registered(email, PASSWORD);

        assertThat(browser.put(
                        "/api/me/password",
                        "{\"currentPassword\":\"wrong-one-123\",\"newPassword\":\"nowe-haslo-123\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("CURRENT_PASSWORD_INVALID"));

        assertThat(browser.put(
                        "/api/me/password",
                        "{\"currentPassword\":\"%s\",\"newPassword\":\"nowe-haslo-123\"}".formatted(PASSWORD)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(browser.get("/api/me")).hasStatus(HttpStatus.OK);
        assertThat(client().login(email, "nowe-haslo-123")).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void theOnlyLoginMethodCannotBeRemoved() {
        TestClient browser = client().registered(uniqueEmail(), PASSWORD);

        assertThat(browser.delete("/api/me/password"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("LAST_LOGIN_METHOD"));
    }

    @Test
    void passwordCanBeRemovedWhenGoogleIsLinkedAndGoogleUnlinkedWhenPasswordExists() {
        String email = uniqueEmail();
        TestClient browser = client().registered(email, PASSWORD);
        long userId =
                linker.resolveUserId(new GoogleIdentity("google-" + email, email, true, "Anna"), null, Locale.of("pl"));

        assertThat(browser.get("/api/me"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.id", id -> assertThat(id).convertTo(Long.class).isEqualTo(userId))
                .hasPathSatisfying("$.providers", p -> assertThat(p).asList().containsExactly("GOOGLE"));

        assertThat(browser.delete("/api/me/providers/google")).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(browser.delete("/api/me/password"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("LAST_LOGIN_METHOD"));
    }

    @Test
    void profileUpdateChangesNameAndLanguage() {
        TestClient browser = client().registered(uniqueEmail(), PASSWORD);

        assertThat(browser.patch("/api/me", "{\"displayName\":\"Ania N.\",\"locale\":\"en\"}"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPathSatisfying("$.displayName", n -> assertThat(n).isEqualTo("Ania N."))
                .hasPathSatisfying("$.locale", l -> assertThat(l).isEqualTo("en"));

        assertThat(browser.patch("/api/me", "{\"locale\":\"de\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void profileEndpointsRequireLogin() {
        assertThat(client().patch("/api/me", "{\"displayName\":\"X\"}")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(client().put("/api/me/password", "{\"newPassword\":\"nowe-haslo-123\"}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
