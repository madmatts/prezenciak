package pl.wishka.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Machine-readable error codes returned in every Problem Details response as the {@code code} property. The frontend
 * translates them ({@code errors.<CODE>}). The backend also resolves {@code error.<CODE>} for the localized
 * {@code detail}.
 *
 * <p>Every code must have a message in both {@code messages_pl} and {@code messages_en} (enforced by a test).
 */
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    CSRF_INVALID(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(HttpStatus.CONFLICT),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR),

    // Accounts and login (Phase 1)
    EMAIL_TAKEN(HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID_OR_EXPIRED(HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    CURRENT_PASSWORD_INVALID(HttpStatus.BAD_REQUEST),
    LAST_LOGIN_METHOD(HttpStatus.CONFLICT),
    PROVIDER_ALREADY_LINKED(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public String messageKey() {
        return "error." + name();
    }

    /** Generic code for framework-raised errors that carry only an HTTP status. */
    public static ErrorCode fromStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 429 -> TOO_MANY_REQUESTS;
            default -> status.is4xxClientError() ? BAD_REQUEST : INTERNAL;
        };
    }
}
