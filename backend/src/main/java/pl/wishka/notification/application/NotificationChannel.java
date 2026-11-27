package pl.wishka.notification.application;

import pl.wishka.notification.api.NotificationType;

/** A way to deliver notifications (email, in-app, push). Implementations must throw on failure so it's retried. */
public interface NotificationChannel {

    Channel channel();

    boolean supports(NotificationType type);

    void deliver(DeliveryTarget target);
}
