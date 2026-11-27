package pl.wishka.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The client's IP address for rate limiting. In production nginx sets {@code X-Forwarded-For} and
 * {@code server.forward-headers-strategy=native} makes {@code getRemoteAddr()} return the real client address.
 */
public final class ClientIp {

    private ClientIp() {}

    public static String of(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
