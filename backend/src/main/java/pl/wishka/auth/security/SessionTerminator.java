package pl.wishka.auth.security;

import org.jspecify.annotations.Nullable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/** Logs a user out everywhere (password reset) or everywhere else (password change), via the session index. */
@Component
public class SessionTerminator {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public SessionTerminator(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    public void terminateAll(long userId) {
        terminateAllExcept(userId, null);
    }

    public void terminateAllExcept(long userId, @Nullable String keepSessionId) {
        sessions.findByPrincipalName(String.valueOf(userId)).keySet().stream()
                .filter(id -> !id.equals(keepSessionId))
                .forEach(sessions::deleteById);
    }
}
