package pl.wishka.notification.api;

import java.util.Locale;

/**
 * Who receives a notification. A registered user is resolved at delivery time (current email and locale); a guest
 * (no account) is addressed by email and the locale they used.
 */
public sealed interface Recipient {

    static Recipient user(long userId) {
        return new User(userId);
    }

    static Recipient email(String email, Locale locale) {
        return new Email(email, locale);
    }

    record User(long userId) implements Recipient {}

    record Email(String email, Locale locale) implements Recipient {}
}
