package pl.wishka.user.web;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** Partial profile update; {@code null} fields stay unchanged. */
public record UpdateMeRequest(
        @Nullable @Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*")
        String displayName,

        @Nullable @Pattern(regexp = "pl|en") String locale) {}
