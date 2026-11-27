package pl.wishka.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import pl.wishka.support.AbstractIntegrationTest;

class PingControllerIT extends AbstractIntegrationTest {

    @Test
    void returnsPolishMessageForPolishLocale() {
        assertThat(mvc.get().uri("/api/public/ping").header(HttpHeaders.ACCEPT_LANGUAGE, "pl-PL"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPathSatisfying("$.message", m -> assertThat(m).isEqualTo("Cześć! Wishka działa."))
                .hasPathSatisfying("$.locale", l -> assertThat(l).isEqualTo("pl"));
    }

    @Test
    void returnsEnglishMessageForEnglishLocale() {
        assertThat(mvc.get().uri("/api/public/ping").header(HttpHeaders.ACCEPT_LANGUAGE, "en-GB,en;q=0.9"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPathSatisfying("$.message", m -> assertThat(m).isEqualTo("Hi! Wishka is running."))
                .hasPathSatisfying("$.locale", l -> assertThat(l).isEqualTo("en"));
    }

    @Test
    void fallsBackToPolishForUnsupportedLocale() {
        assertThat(mvc.get().uri("/api/public/ping").header(HttpHeaders.ACCEPT_LANGUAGE, "de-DE"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPathSatisfying("$.locale", l -> assertThat(l).isEqualTo("pl"));
    }

    @Test
    void fallsBackToPolishWithoutAcceptLanguage() {
        assertThat(mvc.get().uri("/api/public/ping"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPathSatisfying("$.locale", l -> assertThat(l).isEqualTo("pl"));
    }
}
