package docmind.auth;

import docmind.security.JwtService;
import docmind.user.User;
import docmind.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {

        String email = req.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        User user = User.builder()
                .email(email)
                .fullName(req.fullName().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(email);

        return new AuthResponse(
                token,
                email,
                user.getFullName()
        );
    }

    public AuthResponse login(LoginRequest req) {

        String email = req.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .filter(u ->
                        passwordEncoder.matches(
                                req.password(),
                                u.getPasswordHash()
                        )
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password"
                        )
                );

        String token = jwtService.generateToken(email);

        return new AuthResponse(
                token,
                email,
                user.getFullName()
        );
    }
}