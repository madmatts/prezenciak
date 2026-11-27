package pl.wishka.notification.api;

/**
 * Publishes notifications through the transactional outbox: the event is stored in the <b>caller's</b> transaction,
 * so it's sent if and only if the business change commits. Calling it outside a transaction is an error.
 */
public interface NotificationPublisher {

    void publish(NotificationEvent event);
}
