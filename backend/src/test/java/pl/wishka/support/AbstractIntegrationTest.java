package pl.wishka.support;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import pl.wishka.TestcontainersConfiguration;
import pl.wishka.notification.application.OutboxDispatcher;

/**
 * Base class for integration tests: the full application context against PostgreSQL 17 (Testcontainers) with all
 * Flyway migrations applied, and GreenMail as the SMTP server. The context (and the container) is shared between test
 * classes, so tests use unique emails instead of cleaning the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @RegisterExtension
    protected static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected OutboxDispatcher dispatcher;

    /** A fresh client (own cookies, own IP) for one simulated browser. */
    protected TestClient client() {
        return new TestClient(mvc);
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    /** Runs the outbox dispatcher and returns the emails delivered to {@code email} during this test. */
    protected List<MimeMessage> deliverMailTo(String email) {
        dispatcher.dispatchDue();
        return Arrays.stream(greenMail.getReceivedMessages())
                .filter(message -> Mails.recipient(message).equalsIgnoreCase(email))
                .toList();
    }

    protected int outboxEventsFor(String email) {
        Integer count = jdbc.queryForObject("""
                select count(*) from outbox_events o
                left join users u on u.id = o.recipient_user_id
                where u.email = ? or o.recipient_email = ?
                """, Integer.class, email, email);
        return count == null ? 0 : count;
    }
}
