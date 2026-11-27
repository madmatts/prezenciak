package pl.wishka.notification.email;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * A Thymeleaf engine dedicated to emails, separate from any web templates: HTML from {@code templates/email/} and
 * plain text from {@code templates/email-text/}, one file per type and locale (CLAUDE.md §7).
 */
@Configuration(proxyBeanMethods = false)
public class EmailTemplateConfig {

    @Bean
    SpringTemplateEngine emailTemplateEngine() {
        var engine = new SpringTemplateEngine();
        engine.addTemplateResolver(resolver("email/*", ".html", TemplateMode.HTML, 1));
        engine.addTemplateResolver(resolver("email-text/*", ".txt", TemplateMode.TEXT, 2));
        return engine;
    }

    private static ClassLoaderTemplateResolver resolver(String pattern, String suffix, TemplateMode mode, int order) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(suffix);
        resolver.setResolvablePatterns(Set.of(pattern));
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCheckExistence(true);
        resolver.setCacheable(true);
        resolver.setOrder(order);
        return resolver;
    }
}
