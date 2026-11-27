package pl.wishka.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import pl.wishka.notification.api.NotificationEvent;
import pl.wishka.notification.api.NotificationPublisher;
import pl.wishka.notification.api.NotificationType;
import pl.wishka.notification.api.Recipient;
import pl.wishka.support.AbstractIntegrationTest;
import pl.wishka.support.Mails;

class OutboxIT extends AbstractIntegrationTest {

    @Autowired
    NotificationPublisher publisher;

    @Autowired
    TransactionTemplate transactions;

    @Test
    void publishingOutsideATransactionIsAnError() {
        assertThatThrownBy(() -> publisher.publish(event(uniqueEmail(), null)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void rolledBackTransactionLeavesNoEvent() {
        String email = uniqueEmail();

        transactions.executeWithoutResult(status -> {
            publisher.publish(event(email, null));
            status.setRollbackOnly();
        });

        assertThat(outboxEventsFor(email)).isZero();
    }

    @Test
    void guestRecipientGetsEmailInTheirLocaleAndSecretsAreWipedAfterDelivery() {
        String email = uniqueEmail();
        transactions.executeWithoutResult(status -> publisher.publish(event(email, null)));

        var mails = deliverMailTo(email);

        assertThat(mails).hasSize(1);
        assertThat(Mails.subject(mails.getFirst())).isEqualTo("Wishka: set a new password");
        assertThat(Mails.text(mails.getFirst())).contains("Set a new password").doesNotContain("Hi ");
        assertThat(jdbc.queryForObject(
                        "select status || ':' || payload::text from outbox_events where recipient_email = ?",
                        String.class,
                        email))
                .isEqualTo("DONE:{}");
    }

    @Test
    void sameDedupKeyIsPublishedOnce() {
        String email = uniqueEmail();
        String key = "test-dedup-" + email;

        transactions.executeWithoutResult(status -> {
            publisher.publish(event(email, key));
            publisher.publish(event(email, key));
        });

        assertThat(outboxEventsFor(email)).isEqualTo(1);
    }

    @Test
    void alreadyDeliveredChannelIsNotRepeated() {
        String email = uniqueEmail();
        transactions.executeWithoutResult(status -> publisher.publish(event(email, null)));
        Long id = jdbc.queryForObject("select id from outbox_events where recipient_email = ?", Long.class, email);
        jdbc.update("insert into notification_deliveries (outbox_event_id, channel) values (?, 'EMAIL')", id);

        assertThat(deliverMailTo(email)).isEmpty();
        assertThat(jdbc.queryForObject("select status from outbox_events where id = ?", String.class, id))
                .isEqualTo("DONE");
    }

    @Test
    void unknownEventTypeFailsWithoutBlockingOthers() {
        String broken = uniqueEmail();
        String fine = uniqueEmail();
        jdbc.update(
                "insert into outbox_events (type, recipient_email, recipient_locale) values ('NO_SUCH_TYPE', ?, 'pl')",
                broken);
        transactions.executeWithoutResult(status -> publisher.publish(event(fine, null)));

        assertThat(deliverMailTo(fine)).hasSize(1);
        assertThat(jdbc.queryForObject(
                        "select status from outbox_events where recipient_email = ?", String.class, broken))
                .isEqualTo("FAILED");
    }

    private static NotificationEvent event(String email, String dedupKey) {
        return new NotificationEvent(
                NotificationType.ACCOUNT_PASSWORD_RESET,
                Recipient.email(email, Locale.ENGLISH),
                Map.of("actionUrl", "http://wishka.test/reset?token=abc"),
                dedupKey);
    }
}
