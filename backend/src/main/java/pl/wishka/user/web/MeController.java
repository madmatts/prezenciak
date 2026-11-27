package pl.wishka.user.web;

import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wishka.common.error.ApiException;
import pl.wishka.common.error.ErrorCode;
import pl.wishka.common.security.CurrentUserId;
import pl.wishka.user.api.UserAccount;
import pl.wishka.user.api.UserAccounts;

/** The logged-in user's own profile. Owner-facing: never carries reservation data (golden rule 1). */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserAccounts userAccounts;

    public MeController(UserAccounts userAccounts) {
        this.userAccounts = userAccounts;
    }

    @GetMapping
    public MeResponse me(@CurrentUserId long userId) {
        UserAccount account = userAccounts.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        return MeResponse.from(account);
    }

    @PatchMapping
    public MeResponse update(@CurrentUserId long userId, @Valid @RequestBody UpdateMeRequest request) {
        Locale locale = request.locale() == null ? null : Locale.of(request.locale());
        return MeResponse.from(userAccounts.updateProfile(userId, request.displayName(), locale));
    }
}
