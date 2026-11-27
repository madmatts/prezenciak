package pl.wishka.user.api;

import org.jspecify.annotations.Nullable;

/** An external login linked to a user, with the email the provider reported (shown on the profile page). */
public record LinkedLogin(AuthProvider provider, @Nullable String email) {}
