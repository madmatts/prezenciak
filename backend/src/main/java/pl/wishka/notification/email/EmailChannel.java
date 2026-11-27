package pl.wishka.notification.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import pl.wishka.common.config.AppProperties;
import pl.wishka.common.i18n.Messages;
import pl.wishka.common.i18n.SupportedLocales;
import pl.wishka.notification.api.NotificationType;
import pl.wishka.notification.application.Channel;
import pl.wishka.notification.application.DeliveryTarget;
import pl.wishka.notification.application.NotificationChannel;

/**
 * Sends notifications as multipart (HTML + plain text) emails in the recipient's locale. The subject comes from
 * {@code email.<TYPE>.subject}, the bodies from {@code email/<template>_<locale>.html} and
 * {@code email-text/<template>_<locale>.txt}.
 */
@Component
public class EmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templates;
    private final Messages messages;
    private final AppProperties app;

    public EmailChannel(
            JavaMailSender mailSender,
            @Qualifier("emailTemplateEngine") SpringTemplateEngine templates,
            Messages messages,
            AppProperties app) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.messages = messages;
        this.app = app;
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public boolean supports(NotificationType type) {
        return true;
    }

    @Override
    public void deliver(DeliveryTarget target) {
        Locale locale = SupportedLocales.normalize(target.locale());
        String language = locale.getLanguage();
        Context context = new Context(locale, variables(target));
        String base = target.type().template() + "_" + language;
        String html = templates.process("email/" + base, context);
        String text = templates.process("email-text/" + base, context);
        String subject = messages.get("email." + target.type().name() + ".subject", target.params(), locale);
        send(target.email(), subject, text, html);
    }

    private Map<String, Object> variables(DeliveryTarget target) {
        Map<String, Object> variables = new HashMap<>(target.params());
        variables.put("appName", app.name());
        variables.put("appUrl", app.baseUrl().toString());
        variables.put("appHost", app.baseUrl().getHost());
        variables.put("displayName", target.displayName());
        return variables;
    }

    private void send(String to, String subject, String text, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(app.mailFrom(), app.name(), StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailSendException("Could not build email", e);
        }
    }
}
