package pl.wishka.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pl.wishka.common.i18n.Messages;

/**
 * Turns every error into an RFC 9457 Problem Details response with a machine-readable {@code code}, named
 * {@code params} and a {@code detail} localized to the request locale.
 *
 * <p>Security errors raised in filters (401, 403, CSRF) are routed here too, see {@code SecurityConfig}. The catch-all
 * handler never exposes the exception message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Messages messages;
    private final LocaleResolver localeResolver;

    public GlobalExceptionHandler(Messages messages, LocaleResolver localeResolver) {
        this.messages = messages;
        this.localeResolver = localeResolver;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
        return respond(ex.code(), ex.params(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return respond(ErrorCode.UNAUTHORIZED, Map.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorCode code = ex instanceof CsrfException ? ErrorCode.CSRF_INVALID : ErrorCode.FORBIDDEN;
        return respond(code, Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(ErrorCode.INTERNAL, Map.of(), request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getCode()))
                .toList();
        ProblemDetail body = problem(ErrorCode.VALIDATION_FAILED, Map.of(), locale(request));
        body.setProperty("errors", violations);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .headers(headers)
                .body(body);
    }

    /** Framework-raised errors (404, 405, malformed body…) get a generic code derived from their status. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ErrorCode code = ErrorCode.fromStatus(statusCode);
        ProblemDetail problem = problem(code, Map.of(), locale(request));
        problem.setStatus(statusCode.value());
        return new ResponseEntity<>(problem, headers, statusCode);
    }

    private ResponseEntity<ProblemDetail> respond(ErrorCode code, Map<String, Object> params, HttpServletRequest req) {
        ProblemDetail body = problem(code, params, localeResolver.resolveLocale(req));
        return ResponseEntity.status(code.status()).body(body);
    }

    private ProblemDetail problem(ErrorCode code, Map<String, Object> params, Locale locale) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(code.status(), messages.get(code.messageKey(), params, locale));
        problem.setProperty("code", code.name());
        problem.setProperty("params", params);
        return problem;
    }

    private Locale locale(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return localeResolver.resolveLocale(servletRequest.getRequest());
        }
        return request.getLocale();
    }
}
