package docmind.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest req
    ) {
        return authService.register(req);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest req
    ) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public Map<String, String> me(
            Authentication authentication
    ) {
        return Map.of(
                "email",
                authentication.getName()
        );
    }
}