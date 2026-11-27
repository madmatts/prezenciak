package pl.wishka;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.wishka.support.AbstractIntegrationTest;

class FlywayMigrationsIT extends AbstractIntegrationTest {

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allMigrationsAreApplied() {
        var info = flyway.info();

        assertThat(info.pending()).isEmpty();
        assertThat(info.applied()).extracting(m -> m.getVersion().getVersion()).contains("1", "2");
    }

    @Test
    void baselineExtensionsAreInstalled() {
        var extensions = jdbc.queryForList("select extname from pg_extension", String.class);

        assertThat(extensions).contains("citext", "pgcrypto");
    }

    @Test
    void springSessionTablesExist() {
        Integer tables = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name in ('spring_session', 'spring_session_attributes')",
                Integer.class);

        assertThat(tables).isEqualTo(2);
    }
}
