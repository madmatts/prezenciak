package pl.wishka.common.error;

import java.util.Map;

/**
 * Business error with a stable {@link ErrorCode} and named params for the translated message, e.g. {@code new
 * ApiException(ErrorCode.NOT_FOUND)}. Feature modules add their own codes to {@link ErrorCode} as they need them.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> params;

    public ApiException(ErrorCode code) {
        this(code, Map.of());
    }

    public ApiException(ErrorCode code, Map<String, Object> params) {
        super(code.name());
        this.code = code;
        this.params = Map.copyOf(params);
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> params() {
        return params;
    }
}
