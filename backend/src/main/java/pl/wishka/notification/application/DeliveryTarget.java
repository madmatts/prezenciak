package pl.wishka.notification.application;

import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import pl.wishka.notification.api.NotificationType;

/**
 * A notification resolved for delivery: who gets it, in which language, with which parameters.
 *
 * @param displayName the recipient's name, {@code null} for guests without one
 */
public record DeliveryTarget(
        long eventId,
        NotificationType type,
        String email,
        @Nullable String displayName,
        Locale locale,
        Map<String, String> params) {}
