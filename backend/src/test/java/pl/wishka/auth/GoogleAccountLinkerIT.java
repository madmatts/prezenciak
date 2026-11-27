package pl.wishka.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wishka.auth.application.GoogleAccountLinker;
import pl.wishka.auth.application.GoogleAccountLinker.GoogleIdentity;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.error.ErrorCode;
import pl.wishka.support.AbstractIntegrationTest;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;

class GoogleAccountLinkerIT extends AbstractIntegrationTest {

    private static final Locale EN = Locale.ENGLISH;

    @Autowired
    GoogleAccountLinker linker;

    @Autowired
    UserAccounts users;

    @Test
    void newGoogleUserGetsAVerifiedAccountWithoutPassword() {
        String email = uniqueEmail();

        long id = linker.resolveUserId(new GoogleIdentity("sub-" + email, email, true, "Tadeusz"), null, EN);

        UserAccount account = users.findById(id).orElseThrow();
        assertThat(account.emailVerified()).isTrue();
        assertThat(account.hasPassword()).isFalse();
        assertThat(account.displayName()).isEqualTo("Tadeusz");
        assertThat(account.locale()).isEqualTo(EN);
    }

    @Test
    void sameGoogleAccountLogsIntoTheSameUser() {
        String email = uniqueEmail();
        var identity = new GoogleIdentity("sub-" + email, email, true, null);

        long first = linker.resolveUserId(identity, null, EN);
        long second = linker.resolveUserId(identity, null, EN);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void existingPasswordAccountWithSameEmailIsLinkedAndVerified() {
        String email = uniqueEmail();
        client().registered(email, "kot-w-butach-27");
        long existing = users.findByEmail(email).orElseThrow().id();

        long id = linker.resolveUserId(new GoogleIdentity("sub-" + email, email, true, null), null, EN);

        assertThat(id).isEqualTo(existing);
        assertThat(users.findById(id).orElseThrow().emailVerified()).isTrue();
    }

    @Test
    void loggedInUserConnectsGoogleEvenWithDifferentEmail() {
        String email = uniqueEmail();
        client().registered(email, "kot-w-butach-27");
        long current = users.findByEmail(email).orElseThrow().id();

        long id = linker.resolveUserId(new GoogleIdentity("sub-x-" + email, uniqueEmail(), true, null), current, EN);

        assertThat(id).isEqualTo(current);
    }

    @Test
    void googleAccountLinkedToSomeoneElseIsRejected() {
        String owner = uniqueEmail();
        var identity = new GoogleIdentity("sub-" + owner, owner, true, null);
        linker.resolveUserId(identity, null, EN);
        String other = uniqueEmail();
        client().registered(other, "kot-w-butach-27");
        long otherId = users.findByEmail(other).orElseThrow().id();

        assertThatThrownBy(() -> linker.resolveUserId(identity, otherId, EN))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.PROVIDER_ALREADY_LINKED);
    }

    @Test
    void unverifiedGoogleEmailIsRejected() {
        String email = uniqueEmail();

        assertThatThrownBy(() -> linker.resolveUserId(new GoogleIdentity("sub-" + email, email, false, null), null, EN))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
    }
}
