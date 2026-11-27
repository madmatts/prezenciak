package pl.wishka.notification.application;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.wishka.notification.api.NotificationEvent;
import pl.wishka.notification.api.NotificationPublisher;
import pl.wishka.notification.api.Recipient;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OutboxNotificationPublisher implements NotificationPublisher {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public OutboxNotificationPublisher(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(NotificationEvent event) {
        Long userId = null;
        String email = null;
        String locale = null;
        switch (event.recipient()) {
            case Recipient.User user -> userId = user.userId();
            case Recipient.Email guest -> {
                email = guest.email();
                locale = guest.locale().getLanguage();
            }
        }
        jdbc.update(
                """
                insert into outbox_events (type, recipient_user_id, recipient_email, recipient_locale, payload, dedup_key)
                values (?, ?, ?, ?, ?::jsonb, ?)
                on conflict (dedup_key) do nothing
                """,
                event.type().name(),
                userId,
                email,
                locale,
                json.writeValueAsString(Map.copyOf(event.params())),
                event.dedupKey());
    }
}
