package pl.wishka.common.i18n;

import java.util.List;
import java.util.Locale;

/** The UI languages Wishka supports. Polish is the default. */
public final class SupportedLocales {

    public static final Locale POLISH = Locale.of("pl");
    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale DEFAULT = POLISH;
    public static final List<Locale> ALL = List.of(POLISH, ENGLISH);

    private SupportedLocales() {}

    /** Returns the supported locale matching the language of {@code locale}, or the default one. */
    public static Locale normalize(Locale locale) {
        if (locale == null) {
            return DEFAULT;
        }
        return ALL.stream()
                .filter(supported -> supported.getLanguage().equals(locale.getLanguage()))
                .findFirst()
                .orElse(DEFAULT);
    }
}
