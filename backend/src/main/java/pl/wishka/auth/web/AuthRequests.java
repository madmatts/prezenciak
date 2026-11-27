package pl.wishka.auth.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** Request bodies of the account endpoints. Passwords: at least 10 characters (design P0 password field). */
public final class AuthRequests {

    static final int PASSWORD_MIN = 10;
    static final int PASSWORD_MAX = 128;

    private AuthRequests() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,

            @NotBlank @Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
            String password,

            @NotBlank @Size(max = 100) String displayName,
            @AssertTrue boolean acceptPrivacyPolicy,
            @Nullable @Pattern(regexp = "pl|en") String locale) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = PASSWORD_MAX) String password) {}

    public record TokenRequest(@NotBlank @Size(max = 200) String token) {}

    public record PasswordResetRequest(
            @NotBlank @Email @Size(max = 254) String email) {}

    public record PasswordResetConfirmRequest(
            @NotBlank @Size(max = 200) String token,

            @NotBlank @Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
            String password) {}

    public record ChangePasswordRequest(
            @Nullable @Size(max = PASSWORD_MAX) String currentPassword,

            @NotBlank @Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
            String newPassword) {}
}
