package pl.wishka.support;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helpers to read the emails captured by GreenMail. */
public final class Mails {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private Mails() {}

    public static String recipient(MimeMessage message) {
        try {
            return message.getRecipients(Message.RecipientType.TO)[0].toString();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String subject(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String from(MimeMessage message) {
        try {
            return message.getFrom()[0].toString();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String text(MimeMessage message) {
        return part(message, "text/plain");
    }

    public static String html(MimeMessage message) {
        return part(message, "text/html");
    }

    /** The one-time token from the first {@code ?token=} link in the plain-text body. */
    public static String token(MimeMessage message) {
        Matcher matcher = TOKEN.matcher(text(message));
        if (!matcher.find()) {
            throw new AssertionError("No token link in email: " + text(message));
        }
        return matcher.group(1);
    }

    private static String part(Part part, String mimeType) {
        try {
            if (part.isMimeType(mimeType)) {
                return part.getContent().toString();
            }
            if (part.getContent() instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart child = multipart.getBodyPart(i);
                    String found = part(child, mimeType);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        } catch (MessagingException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
