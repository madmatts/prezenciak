package pl.wishka.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * A simulated browser for MockMvc: keeps cookies (session, XSRF-TOKEN) between requests, sends the CSRF header the way
 * the SPA does, and uses its own client IP so in-memory per-IP limits don't leak between tests.
 */
public class TestClient {

    private final MockMvcTester mvc;
    private final Map<String, Cookie> cookies = new HashMap<>();
    private final String ip;
    private String language = "pl";

    TestClient(MockMvcTester mvc) {
        this.mvc = mvc;
        this.ip = "10.%d.%d.%d"
                .formatted(
                        ThreadLocalRandom.current().nextInt(256),
                        ThreadLocalRandom.current().nextInt(256),
                        ThreadLocalRandom.current().nextInt(1, 255));
    }

    public TestClient language(String language) {
        this.language = language;
        return this;
    }

    public MvcTestResult get(String uri) {
        return exchange(mvc.get().uri(uri), false);
    }

    public MvcTestResult post(String uri, String json) {
        return exchange(
                mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json), true);
    }

    public MvcTestResult post(String uri) {
        return exchange(mvc.post().uri(uri), true);
    }

    public MvcTestResult put(String uri, String json) {
        return exchange(
                mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json), true);
    }

    public MvcTestResult patch(String uri, String json) {
        return exchange(
                mvc.patch().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json), true);
    }

    public MvcTestResult delete(String uri) {
        return exchange(mvc.delete().uri(uri), true);
    }

    /** A state-changing request without the CSRF header, to prove it's rejected. */
    public MvcTestResult postWithoutCsrf(String uri, String json) {
        return exchange(
                mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json), false);
    }

    public MvcTestResult register(String email, String password, String displayName) {
        return post("/api/auth/register", """
                {"email":"%s","password":"%s","displayName":"%s","acceptPrivacyPolicy":true,"locale":"%s"}
                """.formatted(email, password, displayName, language));
    }

    public MvcTestResult login(String email, String password) {
        return post("/api/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, password));
    }

    public TestClient registered(String email, String password) {
        assertThat(register(email, password, "Anna")).hasStatus(HttpStatus.CREATED);
        return this;
    }

    public String sessionCookie() {
        Cookie cookie = cookies.get("WISHKA_SESSION");
        return cookie == null ? null : cookie.getValue();
    }

    public List<String> setCookieHeaders(MvcTestResult result) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    }

    private MvcTestResult exchange(MockMvcTester.MockMvcRequestBuilder builder, boolean withCsrf) {
        if (withCsrf && !cookies.containsKey("XSRF-TOKEN")) {
            exchange(mvc.get().uri("/api/auth/csrf"), false);
        }
        builder.header(HttpHeaders.ACCEPT_LANGUAGE, language).with(request -> {
            request.setRemoteAddr(ip);
            return request;
        });
        if (!cookies.isEmpty()) {
            builder.cookie(cookies.values().toArray(Cookie[]::new));
        }
        if (withCsrf && cookies.containsKey("XSRF-TOKEN")) {
            builder.header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN").getValue());
        }
        MvcTestResult result = builder.exchange();
        for (Cookie cookie : result.getResponse().getCookies()) {
            if (cookie.getMaxAge() == 0) {
                cookies.remove(cookie.getName());
            } else {
                cookies.put(cookie.getName(), cookie);
            }
        }
        return result;
    }
}
