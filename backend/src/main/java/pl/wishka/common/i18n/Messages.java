package pl.wishka.common.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import pl.wishka.common.config.AppProperties;

/**
 * Resolves backend texts with <b>named</b> parameters, e.g. {@code "Hi! {appName} is running."}.
 *
 * <p>The same placeholder style as vue-i18n on the frontend, so translators see one convention. {@code {appName}} is
 * always available, so the brand name never has to be written into a translation.
 */
@Component
public class Messages {

    public static final String APP_NAME_PARAM = "appName";

    private final MessageSource messageSource;
    private final AppProperties appProperties;

    public Messages(MessageSource messageSource, AppProperties appProperties) {
        this.messageSource = messageSource;
        this.appProperties = appProperties;
    }

    public String get(String key, Locale locale) {
        return get(key, Map.of(), locale);
    }

    public String get(String key, Map<String, ?> params, Locale locale) {
        String template = messageSource.getMessage(key, null, SupportedLocales.normalize(locale));
        return interpolate(template, withAppName(params));
    }

    private Map<String, Object> withAppName(Map<String, ?> params) {
        Map<String, Object> all = new HashMap<>(params);
        all.putIfAbsent(APP_NAME_PARAM, appProperties.name());
        return all;
    }

    private static String interpolate(String template, Map<String, Object> params) {
        String result = template;
        for (Map.Entry<String, Object> param : params.entrySet()) {
            result = result.replace("{" + param.getKey() + "}", String.valueOf(param.getValue()));
        }
        return result;
    }
}
