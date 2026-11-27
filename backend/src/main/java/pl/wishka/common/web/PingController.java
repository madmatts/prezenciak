package pl.wishka.common.web;

import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wishka.common.i18n.Messages;
import pl.wishka.common.i18n.SupportedLocales;

/** Smoke endpoint proving that the API is up and i18n works end to end. */
@RestController
@RequestMapping("/api/public/ping")
public class PingController {

    private final Messages messages;

    public PingController(Messages messages) {
        this.messages = messages;
    }

    @GetMapping
    public PingResponse ping(Locale locale) {
        Locale resolved = SupportedLocales.normalize(locale);
        return new PingResponse(messages.get("ping.message", resolved), resolved.getLanguage());
    }

    public record PingResponse(String message, String locale) {}
}
