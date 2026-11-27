package pl.wishka.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import pl.wishka.support.AbstractIntegrationTest;

class ProblemDetailsIT extends AbstractIntegrationTest {

    private static final String BASE = "/api/public/test-errors";

    @Test
    void validationErrorListsFieldsAndConstraintCodes() {
        var result = mvc.post()
                .uri(BASE + "/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"email\":\"not-an-email\"}");

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("VALIDATION_FAILED"))
                .hasPathSatisfying(
                        "$.errors[*].field", f -> assertThat(f).asList().containsExactlyInAnyOrder("name", "email"))
                .hasPathSatisfying(
                        "$.errors[*].code", c -> assertThat(c).asList().containsExactlyInAnyOrder("NotBlank", "Email"));
    }

    @Test
    void validRequestPasses() {
        var result = mvc.post()
                .uri(BASE + "/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Anna\",\"email\":\"anna@example.com\"}");

        assertThat(result).hasStatus(HttpStatus.OK);
    }

    @Test
    void businessErrorCarriesCodeAndParams() {
        assertThat(mvc.get().uri(BASE + "/business").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("CONFLICT"))
                .hasPathSatisfying("$.params.resource", p -> assertThat(p).isEqualTo("sample"))
                .hasPathSatisfying(
                        "$.detail",
                        d -> assertThat(d).isEqualTo("The data was changed in the meantime. Refresh and try again."));
    }

    @Test
    void detailIsLocalizedToPolish() {
        assertThat(mvc.get().uri(BASE + "/business").header(HttpHeaders.ACCEPT_LANGUAGE, "pl"))
                .bodyJson()
                .hasPathSatisfying(
                        "$.detail",
                        d -> assertThat(d)
                                .isEqualTo("Dane zostały w międzyczasie zmienione. Odśwież i spróbuj ponownie."));
    }

    @Test
    void unexpectedErrorNeverLeaksExceptionMessage() {
        var result = mvc.get().uri(BASE + "/unexpected").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("INTERNAL"));
        assertThat(result).bodyText().doesNotContain("secret internal detail");
    }

    @Test
    void unknownPathReturnsNotFoundProblem() {
        assertThat(mvc.get().uri("/api/public/does-not-exist"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("NOT_FOUND"));
    }

    @Test
    void protectedPathWithoutSessionReturnsUnauthorized() {
        assertThat(mvc.get().uri("/api/me"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("UNAUTHORIZED"));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() {
        var result = mvc.post()
                .uri(BASE + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Anna\"}");

        assertThat(result)
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .hasPathSatisfying("$.code", c -> assertThat(c).isEqualTo("CSRF_INVALID"));
    }
}
