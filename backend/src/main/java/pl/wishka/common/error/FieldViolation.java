package pl.wishka.common.error;

/**
 * One invalid input field in a {@code VALIDATION_FAILED} response.
 *
 * @param field request field name, e.g. {@code email}
 * @param code constraint name, e.g. {@code NotBlank}; the frontend translates it as {@code validation.<code>}
 */
public record FieldViolation(String field, String code) {}
