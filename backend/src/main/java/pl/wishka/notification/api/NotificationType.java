package pl.wishka.notification.api;

/**
 * Every kind of notification Wishka sends. Phase 1 has the account emails; later phases add circle, reservation and
 * list events (CLAUDE.md §3).
 */
public enum NotificationType {
    ACCOUNT_VERIFY_EMAIL("verify-email", true, true),
    ACCOUNT_PASSWORD_RESET("password-reset", true, true);

    private final String template;
    private final boolean mandatory;
    private final boolean containsSecrets;

    NotificationType(String template, boolean mandatory, boolean containsSecrets) {
        this.template = template;
        this.mandatory = mandatory;
        this.containsSecrets = containsSecrets;
    }

    /** Template base name: {@code templates/email/<template>_<locale>.html} and {@code email-text/…txt}. */
    public String template() {
        return template;
    }

    /** Account emails ignore notification preferences: the user can't opt out of them. */
    public boolean mandatory() {
        return mandatory;
    }

    /** The payload holds a one-time link; it's wiped from the outbox once delivered. */
    public boolean containsSecrets() {
        return containsSecrets;
    }
}
