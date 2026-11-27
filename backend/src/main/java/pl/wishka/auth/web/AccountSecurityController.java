package pl.wishka.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wishka.auth.application.AccountSecurityService;
import pl.wishka.auth.web.AuthRequests.ChangePasswordRequest;
import pl.wishka.common.security.CurrentUserId;

/** Sign-in methods on the profile page (D-38). The last remaining method can never be removed. */
@RestController
@RequestMapping("/api/me")
public class AccountSecurityController {

    private final AccountSecurityService accountSecurity;

    public AccountSecurityController(AccountSecurityService accountSecurity) {
        this.accountSecurity = accountSecurity;
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @CurrentUserId long userId, @Valid @RequestBody ChangePasswordRequest body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        accountSecurity.changePassword(
                userId, body.currentPassword(), body.newPassword(), session == null ? null : session.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/password")
    public ResponseEntity<Void> removePassword(@CurrentUserId long userId) {
        accountSecurity.removePassword(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/providers/google")
    public ResponseEntity<Void> unlinkGoogle(@CurrentUserId long userId) {
        accountSecurity.unlinkGoogle(userId);
        return ResponseEntity.noContent().build();
    }
}
