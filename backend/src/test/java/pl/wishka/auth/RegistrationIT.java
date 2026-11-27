package pl.wishka.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import pl.wishka.support.AbstractIntegrationTest;
import pl.wishka.support.Mails;
import pl.wishka.support.TestClient;

class RegistrationIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "kot-w-butach-27";

    @Test
    void registrationLogsInAndSendsPolishVerificationEmail() {
        String email = uniqueEmail();
        TestClient browser = client().language("pl");

        assertThat(browser.register(email, PASSWORD, "Ania")).hasStatus(HttpStatus.CREATED);

        assertThat(browser.get("/api/me"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPathSatisfying("$.email", e -> assertThat(e).isEqualTo(email))
                .hasPathSatisfying("$.locale", l -> assertThat(l).isEqualTo("pl"))
                .hasPathSatisfying("$.emailVerified", v -> assertThat(v).isEqualTo(false))
                .hasPathSatisfying("$.hasPassword", v -> assertThat(v).isEqualTo(true));

        List<MimeMessage> mails = deliverMailTo(email);
        assertThat(mails).hasSize(1);
        MimeMessage mail = mails.getFirst();
        assertThat(Mails.subject(mail)).isEqualTo("Wishka: potwierdź swój adres e-mail");
        assertThat(Mails.from(mail)).contains("Wishka").contains("no-reply@wishka.pl");
        assertThat(Mails.text(mail)).contains("Cześć Ania").contains("http://wishka.test/verify?token=");
        assertThat(Mails.html(mail)).contains("Potwierdź adres e-mail").contains("lang=\"pl\"");
    }

    @Test
    void englishRegistrationGetsEnglishEmail() {
        String email = uniqueEmail();

        client().language("en").register(email, PASSWORD, "Anna");

        MimeMessage mail = deliverMailTo(email).getFirst();
        assertThat(Mails.subject(mail)).isEqualTo("Wishka: confirm your email address");
        assertThat(Mails.text(mail)).contains("Hi Anna");
    }

    @Test
    void verificationLinkVerifiesOnceAndOnlyOnce() {
        String email = uniqueEmail();
        TestClient browser = client().registered(email, PASSWORD);
        String token = Mails.token(deliverMailTo(email).getFirst());

        assertThat(client().post("/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(token)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(browser.get("/api/me"))
                .bodyJson()
                .hasPathSatisfying("$.emailVerified", v -> assertThat(v).isEqualTo(true));

        assertThat(client().post("/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(token)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("TOKEN_INVALID_OR_EXPIRED"));
    }

    @Test
    void unknownVerificationTokenIsRejected() {
        assertThat(client().post("/api/auth/verify-email", "{\"token\":\"made-up\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("TOKEN_INVALID_OR_EXPIRED"));
    }

    @Test
    void resendIssuesANewLinkAndInvalidatesTheOldOne() {
        String email = uniqueEmail();
        TestClient browser = client().registered(email, PASSWORD);
        String first = Mails.token(deliverMailTo(email).getFirst());
        jdbc.update("delete from email_rate_limits where email = ?", email);

        assertThat(browser.post("/api/auth/verify-email/resend")).hasStatus(HttpStatus.ACCEPTED);
        assertThat(browser.post("/api/auth/verify-email/resend"))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("TOO_MANY_REQUESTS"));

        List<MimeMessage> mails = deliverMailTo(email);
        assertThat(mails).hasSize(2);
        String second = Mails.token(mails.get(1));
        assertThat(client().post("/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(first)))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(client().post("/api/auth/verify-email", "{\"token\":\"%s\"}".formatted(second)))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void duplicateEmailIsRejectedAndSendsNothing() {
        String email = uniqueEmail();
        client().registered(email, PASSWORD);
        int eventsBefore = outboxEventsFor(email);

        assertThat(client().register(email.toUpperCase(), PASSWORD, "Ktoś"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("EMAIL_TAKEN"));
        assertThat(outboxEventsFor(email)).isEqualTo(eventsBefore);
    }

    @Test
    void invalidRegistrationListsFieldErrors() {
        var result = client().post("/api/auth/register", """
                        {"email":"not-an-email","password":"short","displayName":"","acceptPrivacyPolicy":false}
                        """);

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("VALIDATION_FAILED"))
                .hasPathSatisfying(
                        "$.errors[*].field",
                        f -> assertThat(f)
                                .asList()
                                .contains("email", "password", "displayName", "acceptPrivacyPolicy"));
    }
}
