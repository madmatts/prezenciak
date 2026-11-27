package pl.wishka.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import pl.wishka.notification.api.NotificationEvent;
import pl.wishka.notification.api.NotificationPublisher;
import pl.wishka.notification.api.NotificationType;
import pl.wishka.notification.api.Recipient;
import pl.wishka.support.AbstractIntegrationTest;

/** SMTP pointed at a closed port: every delivery fails, so retries and backoff can be observed. */
@TestPropertySource(properties = {"spring.mail.port=1", "spring.mail.properties.mail.smtp.connectiontimeout=500"})
class OutboxRetryIT extends AbstractIntegrationTest {

    @Autowired
    NotificationPublisher publisher;

    @Autowired
    TransactionTemplate transactions;

    @Test
    void failedDeliveryIsRetriedLaterWithBackoff() {
        String email = uniqueEmail();
        transactions.executeWithoutResult(status -> publisher.publish(event(email)));
        Instant before = Instant.now();

        dispatcher.dispatchDue();

        var row = jdbc.queryForMap(
                "select status, attempts, next_attempt_at, last_error from outbox_events where recipient_email = ?",
                email);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("attempts")).isEqualTo(1);
        assertThat(((Timestamp) row.get("next_attempt_at")).toInstant()).isAfter(before.plus(Duration.ofSeconds(25)));
        assertThat((String) row.get("last_error")).isNotBlank();
    }

    @Test
    void eventIsMarkedFailedAfterMaxAttempts() {
        String email = uniqueEmail();
        transactions.executeWithoutResult(status -> publisher.publish(event(email)));
        jdbc.update("update outbox_events set attempts = 7 where recipient_email = ?", email);

        dispatcher.dispatchDue();

        assertThat(jdbc.queryForObject(
                        "select status || ':' || attempts from outbox_events where recipient_email = ?",
                        String.class,
                        email))
                .isEqualTo("FAILED:8");
    }

    private static NotificationEvent event(String email) {
        return NotificationEvent.of(
                NotificationType.ACCOUNT_PASSWORD_RESET,
                Recipient.email(email, Locale.of("pl")),
                Map.of("actionUrl", "http://wishka.test/reset?token=abc"));
    }
}
