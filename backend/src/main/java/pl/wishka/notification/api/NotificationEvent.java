package pl.wishka.notification.api;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A notification to deliver.
 *
 * @param params values for the templates and messages, e.g. {@code actionUrl}
 * @param dedupKey optional key that makes publishing idempotent (a second event with the same key is ignored)
 */
public record NotificationEvent(
        NotificationType type,
        Recipient recipient,
        Map<String, String> params,
        @Nullable String dedupKey) {

    public NotificationEvent {
        params = Map.copyOf(params);
    }

    public static NotificationEvent of(NotificationType type, Recipient recipient, Map<String, String> params) {
        return new NotificationEvent(type, recipient, params, null);
    }
}
