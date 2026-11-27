package pl.wishka.user.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import pl.wishka.user.api.AuthProvider;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "password_hash")
    private @Nullable String passwordHash;

    @Column(nullable = false, length = 2)
    private String locale;

    @Column(name = "email_verified_at")
    private @Nullable Instant emailVerifiedAt;

    @Column(name = "privacy_accepted_at")
    private @Nullable Instant privacyAcceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UserAuthProviderLink> providers = new HashSet<>();

    protected User() {}

    public User(String email, String displayName, @Nullable String passwordHash, Locale locale, Instant now) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.locale = locale.getLanguage();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void rename(String newDisplayName, Instant now) {
        this.displayName = newDisplayName;
        this.updatedAt = now;
    }

    public void changeLocale(Locale newLocale, Instant now) {
        this.locale = newLocale.getLanguage();
        this.updatedAt = now;
    }

    public void changePassword(String newPasswordHash, Instant now) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = now;
    }

    public void removePassword(Instant now) {
        this.passwordHash = null;
        this.updatedAt = now;
    }

    public void markEmailVerified(Instant now) {
        if (emailVerifiedAt == null) {
            this.emailVerifiedAt = now;
            this.updatedAt = now;
        }
    }

    public void acceptPrivacyPolicy(Instant now) {
        this.privacyAcceptedAt = now;
    }

    public void linkProvider(AuthProvider provider, String subject, @Nullable String providerEmail, Instant now) {
        providers.add(new UserAuthProviderLink(this, provider, subject, providerEmail, now));
        this.updatedAt = now;
    }

    public void unlinkProvider(AuthProvider provider, Instant now) {
        providers.removeIf(link -> link.provider() == provider);
        this.updatedAt = now;
    }

    public Optional<UserAuthProviderLink> providerLink(AuthProvider provider) {
        return providers.stream().filter(link -> link.provider() == provider).findFirst();
    }

    public int loginMethodCount() {
        return (passwordHash != null ? 1 : 0) + providers.size();
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public @Nullable String passwordHash() {
        return passwordHash;
    }

    public Locale locale() {
        return Locale.of(locale);
    }

    public boolean emailVerified() {
        return emailVerifiedAt != null;
    }

    public Set<UserAuthProviderLink> providers() {
        return Set.copyOf(providers);
    }
}
