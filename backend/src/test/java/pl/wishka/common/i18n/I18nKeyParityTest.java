package pl.wishka.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import pl.wishka.common.error.ErrorCode;
import pl.wishka.notification.api.NotificationType;

/** Golden rule 4: every backend text exists in Polish and English, in the same commit. */
class I18nKeyParityTest {

    private static final List<Path> EMAIL_TEMPLATE_DIRS =
            List.of(Path.of("src/main/resources/templates/email"), Path.of("src/main/resources/templates/email-text"));

    @Test
    void polishAndEnglishMessagesHaveTheSameKeys() throws IOException {
        Set<String> polish = keys("i18n/messages_pl.properties");
        Set<String> english = keys("i18n/messages_en.properties");

        assertThat(english).as("keys missing in messages_en").containsAll(polish);
        assertThat(polish).as("keys missing in messages_pl").containsAll(english);
    }

    @Test
    void noMessageIsBlank() throws IOException {
        for (String file : new String[] {"i18n/messages_pl.properties", "i18n/messages_en.properties"}) {
            Properties properties = load(file);
            assertThat(properties.stringPropertyNames())
                    .as("blank values in %s", file)
                    .allSatisfy(key -> assertThat(properties.getProperty(key)).isNotBlank());
        }
    }

    @Test
    void everyErrorCodeHasAMessage() throws IOException {
        Set<String> expected =
                Arrays.stream(ErrorCode.values()).map(ErrorCode::messageKey).collect(Collectors.toSet());

        assertThat(keys("i18n/messages_pl.properties")).containsAll(expected);
        assertThat(keys("i18n/messages_en.properties")).containsAll(expected);
    }

    @Test
    void brandNameIsNotHardCodedInMessages() throws IOException {
        for (String file : new String[] {"i18n/messages_pl.properties", "i18n/messages_en.properties"}) {
            Properties properties = load(file);
            assertThat(properties.values())
                    .as("use {appName} instead of the brand name in %s", file)
                    .allSatisfy(value -> assertThat(value.toString()).doesNotContainIgnoringCase("wishka"));
        }
    }

    @Test
    void everyEmailTemplateExistsInBothLocales() throws IOException {
        for (Path dir : EMAIL_TEMPLATE_DIRS) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            Set<String> templates;
            try (Stream<Path> files = Files.list(dir)) {
                templates = files.map(p -> p.getFileName().toString()).collect(Collectors.toSet());
            }
            for (String template : templates) {
                if (template.contains("_pl.")) {
                    assertThat(templates).as(dir.toString()).contains(template.replace("_pl.", "_en."));
                } else if (template.contains("_en.")) {
                    assertThat(templates).as(dir.toString()).contains(template.replace("_en.", "_pl."));
                }
            }
        }
    }

    @Test
    void everyNotificationTypeHasSubjectsAndTemplatesInBothLocales() throws IOException {
        for (NotificationType type : NotificationType.values()) {
            for (String locale : new String[] {"pl", "en"}) {
                assertThat(keys("i18n/messages_" + locale + ".properties"))
                        .contains("email." + type.name() + ".subject");
                assertThat(Path.of("src/main/resources/templates/email/" + type.template() + "_" + locale + ".html"))
                        .exists();
                assertThat(Path.of(
                                "src/main/resources/templates/email-text/" + type.template() + "_" + locale + ".txt"))
                        .exists();
            }
        }
    }

    private static Set<String> keys(String resource) throws IOException {
        return load(resource).stringPropertyNames();
    }

    private static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = I18nKeyParityTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as(resource).isNotNull();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
