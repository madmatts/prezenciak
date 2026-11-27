package pl.wishka.common.i18n;

import java.nio.charset.StandardCharsets;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Backend i18n: {@code i18n/messages_pl.properties} and {@code i18n/messages_en.properties}.
 *
 * <p>Locale for anonymous requests comes from {@code Accept-Language}, limited to the supported locales, with Polish as
 * the default. From Phase 1 the logged-in user's profile locale takes precedence.
 */
@Configuration(proxyBeanMethods = false)
public class I18nConfig {

    @Bean
    MessageSource messageSource() {
        var messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(SupportedLocales.DEFAULT);
        return messageSource;
    }

    @Bean
    LocaleResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SupportedLocales.ALL);
        resolver.setDefaultLocale(SupportedLocales.DEFAULT);
        return resolver;
    }
}
