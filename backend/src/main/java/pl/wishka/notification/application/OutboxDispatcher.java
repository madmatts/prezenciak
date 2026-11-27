package pl.wishka.notification.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pl.wishka.notification.api.NotificationType;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Delivers outbox events to the channels (CLAUDE.md §3).
 *
 * <p>Each run locks a batch of due events with {@code FOR UPDATE SKIP LOCKED} (safe if several instances ever run),
 * delivers every channel that hasn't succeeded yet, and records each success in {@code notification_deliveries}, so a
 * retry never repeats a channel that already worked. Failures are retried with exponential backoff until
 * {@code maxAttempts}, then the event is marked {@code FAILED}.
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final TypeReference<Map<String, String>> PARAMS = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final List<NotificationChannel> channels;
    private final UserAccounts userAccounts;
    private final NotificationProperties properties;
    private final JsonMapper json;
    private final Clock clock;

    public OutboxDispatcher(
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            List<NotificationChannel> channels,
            UserAccounts userAccounts,
            NotificationProperties properties,
            JsonMapper json,
            Clock clock) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.channels = channels;
        this.userAccounts = userAccounts;
        this.properties = properties;
        this.json = json;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${app.notifications.dispatch-interval}",
            initialDelayString = "${app.notifications.dispatch-interval}")
    public void dispatchDue() {
        int processed;
        do {
            Integer count = transactions.execute(status -> dispatchBatch());
            processed = count == null ? 0 : count;
        } while (processed == properties.batchSize());
    }

    private int dispatchBatch() {
        List<OutboxRow> due =
                jdbc.query("""
                select id, type, recipient_user_id, recipient_email, recipient_locale, payload::text as payload, attempts
                from outbox_events
                where status = 'PENDING' and next_attempt_at <= ?
                order by id
                limit ?
                for update skip locked
                """, OutboxDispatcher::mapRow, Timestamp.from(clock.instant()), properties.batchSize());
        due.forEach(this::process);
        return due.size();
    }

    private void process(OutboxRow row) {
        NotificationType type;
        try {
            type = NotificationType.valueOf(row.type());
        } catch (IllegalArgumentException unknownType) {
            markFailed(row, "Unknown notification type " + row.type());
            return;
        }
        Optional<DeliveryTarget> target = resolve(row, type);
        if (target.isEmpty()) {
            // The recipient's account no longer exists: nothing to deliver.
            markDone(row.id(), type);
            return;
        }
        try {
            for (NotificationChannel channel : channels) {
                if (channel.supports(type) && !alreadyDelivered(row.id(), channel.channel())) {
                    channel.deliver(target.get());
                    recordDelivery(row.id(), channel.channel());
                }
            }
            markDone(row.id(), type);
        } catch (RuntimeException e) {
            log.warn("Delivery of outbox event {} ({}) failed, attempt {}", row.id(), type, row.attempts() + 1, e);
            markRetry(row, e);
        }
    }

    private Optional<DeliveryTarget> resolve(OutboxRow row, NotificationType type) {
        Map<String, String> params = json.readValue(row.payload(), PARAMS);
        if (row.recipientUserId() != null) {
            Optional<UserAccount> account = userAccounts.findById(row.recipientUserId());
            return account.map(user ->
                    new DeliveryTarget(row.id(), type, user.email(), user.displayName(), user.locale(), params));
        }
        Locale locale = Locale.of(row.recipientLocale() == null ? "pl" : row.recipientLocale());
        return Optional.of(new DeliveryTarget(row.id(), type, row.recipientEmail(), null, locale, params));
    }

    private boolean alreadyDelivered(long eventId, Channel channel) {
        Integer count = jdbc.queryForObject(
                "select count(*) from notification_deliveries where outbox_event_id = ? and channel = ?",
                Integer.class,
                eventId,
                channel.name());
        return count != null && count > 0;
    }

    private void recordDelivery(long eventId, Channel channel) {
        jdbc.update(
                "insert into notification_deliveries (outbox_event_id, channel, delivered_at) values (?, ?, ?)",
                eventId,
                channel.name(),
                Timestamp.from(clock.instant()));
    }

    private void markDone(long eventId, NotificationType type) {
        // One-time links must not linger in the database once they've been sent.
        String payloadUpdate = type.containsSecrets() ? ", payload = '{}'::jsonb" : "";
        jdbc.update(
                "update outbox_events set status = 'DONE', processed_at = ?" + payloadUpdate + " where id = ?",
                Timestamp.from(clock.instant()),
                eventId);
    }

    private void markRetry(OutboxRow row, RuntimeException error) {
        int attempts = row.attempts() + 1;
        if (attempts >= properties.maxAttempts()) {
            markFailed(row, describe(error));
            return;
        }
        Instant next = clock.instant().plus(properties.backoffAfter(attempts));
        jdbc.update(
                "update outbox_events set attempts = ?, next_attempt_at = ?, last_error = ? where id = ?",
                attempts,
                Timestamp.from(next),
                describe(error),
                row.id());
    }

    private void markFailed(OutboxRow row, String error) {
        log.error("Outbox event {} ({}) failed permanently: {}", row.id(), row.type(), error);
        jdbc.update(
                "update outbox_events set status = 'FAILED', attempts = ?, last_error = ?, processed_at = ? where id = ?",
                row.attempts() + 1,
                truncate(error),
                Timestamp.from(clock.instant()),
                row.id());
    }

    private static String describe(RuntimeException error) {
        return truncate(error.getClass().getSimpleName() + ": " + error.getMessage());
    }

    private static String truncate(String text) {
        return text.length() <= 1000 ? text : text.substring(0, 1000);
    }

    private static OutboxRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        long userId = rs.getLong("recipient_user_id");
        Long recipientUserId = rs.wasNull() ? null : userId;
        return new OutboxRow(
                rs.getLong("id"),
                rs.getString("type"),
                recipientUserId,
                rs.getString("recipient_email"),
                rs.getString("recipient_locale"),
                rs.getString("payload"),
                rs.getInt("attempts"));
    }

    private record OutboxRow(
            long id,
            String type,
            @Nullable Long recipientUserId,
            @Nullable String recipientEmail,
            @Nullable String recipientLocale,
            String payload,
            int attempts) {}
}
