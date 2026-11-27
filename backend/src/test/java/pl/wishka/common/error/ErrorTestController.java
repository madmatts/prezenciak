package pl.wishka.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that raise each kind of error handled by {@link GlobalExceptionHandler}. */
@RestController
@RequestMapping("/api/public/test-errors")
class ErrorTestController {

    @PostMapping("/validate")
    void validate(@Valid @RequestBody SampleRequest request) {}

    @GetMapping("/business")
    void business() {
        throw new ApiException(ErrorCode.CONFLICT, Map.of("resource", "sample"));
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException("secret internal detail");
    }

    record SampleRequest(@NotBlank String name, @Email String email) {}
}
