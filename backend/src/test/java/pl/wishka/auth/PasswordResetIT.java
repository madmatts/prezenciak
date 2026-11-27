package pl.wishka.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import pl.wishka.support.AbstractIntegrationTest;
import pl.wishka.support.Mails;
import pl.wishka.support.TestClient;

class PasswordResetIT extends AbstractIntegrationTest {

    private static final String OLD = "kot-w-butach-27";
    private static final String NEW = "pies-w-kaloszach-42";

    @Test
    void unknownEmailGetsTheSameAnswerAndNoEmail() {
        String email = uniqueEmail();

        assertThat(client().post("/api/auth/password-reset/request", "{\"email\":\"%s\"}".formatted(email)))
                .hasStatus(HttpStatus.ACCEPTED);

        assertThat(outboxEventsFor(email)).isZero();
        assertThat(deliverMailTo(email)).isEmpty();
    }

    @Test
    void resetChangesPasswordAndLogsOutEverySession() {
        String email = uniqueEmail();
        TestClient loggedIn = client().registered(email, OLD);
        deliverMailTo(email);

        assertThat(client().post("/api/auth/password-reset/request", "{\"email\":\"%s\"}".formatted(email)))
                .hasStatus(HttpStatus.ACCEPTED);
        var mails = deliverMailTo(email);
        var resetMail = mails.getLast();
        assertThat(Mails.subject(resetMail)).isEqualTo("Wishka: ustaw nowe hasło");
        assertThat(Mails.text(resetMail)).contains("http://wishka.test/reset?token=");
        String token = Mails.token(resetMail);

        assertThat(client().post(
                                "/api/auth/password-reset/confirm",
                                "{\"token\":\"%s\",\"password\":\"%s\"}".formatted(token, NEW)))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(loggedIn.get("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(client().login(email, OLD)).hasStatus(HttpStatus.UNAUTHORIZED);
        TestClient fresh = client();
        assertThat(fresh.login(email, NEW)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(fresh.get("/api/me"))
                .bodyJson()
                .hasPathSatisfying("$.emailVerified", v -> assertThat(v).isEqualTo(true));
    }

    @Test
    void resetLinkWorksOnlyOnce() {
        String email = uniqueEmail();
        client().registered(email, OLD);
        client().post("/api/auth/password-reset/request", "{\"email\":\"%s\"}".formatted(email));
        String token = Mails.token(deliverMailTo(email).getLast());
        String body = "{\"token\":\"%s\",\"password\":\"%s\"}".formatted(token, NEW);
        client().post("/api/auth/password-reset/confirm", body);

        assertThat(client().post("/api/auth/password-reset/confirm", body))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("TOKEN_INVALID_OR_EXPIRED"));
    }

    @Test
    void expiredResetLinkIsRejected() {
        String email = uniqueEmail();
        client().registered(email, OLD);
        client().post("/api/auth/password-reset/request", "{\"email\":\"%s\"}".formatted(email));
        String token = Mails.token(deliverMailTo(email).getLast());
        jdbc.update(
                "update password_reset_tokens set expires_at = now() - interval '1 minute' where user_id ="
                        + " (select id from users where email = ?)",
                email);

        assertThat(client().post(
                                "/api/auth/password-reset/confirm",
                                "{\"token\":\"%s\",\"password\":\"%s\"}".formatted(token, NEW)))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void tooShortNewPasswordIsRejected() {
        assertThat(client().post("/api/auth/password-reset/confirm", "{\"token\":\"x\",\"password\":\"short\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("VALIDATION_FAILED"));
    }
}
