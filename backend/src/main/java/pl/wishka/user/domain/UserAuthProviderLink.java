package pl.wishka.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import pl.wishka.user.api.AuthProvider;

/** An external login (Google) linked to a user. */
@Entity
@Table(name = "user_auth_providers")
public class UserAuthProviderLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "citext")
    private @Nullable String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAuthProviderLink() {}

    UserAuthProviderLink(User user, AuthProvider provider, String subject, @Nullable String email, Instant createdAt) {
        this.user = user;
        this.provider = provider;
        this.subject = subject;
        this.email = email;
        this.createdAt = createdAt;
    }

    public AuthProvider provider() {
        return provider;
    }

    public String subject() {
        return subject;
    }
}
