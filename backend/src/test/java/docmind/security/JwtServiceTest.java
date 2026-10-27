package docmind.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-chars-long";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000, false);
    }

    @Test
    void generatesAndExtractsSubject() {
        String token = jwtService.generateToken("alice@example.com");
        assertThat(jwtService.extractEmail(token)).isEqualTo("alice@example.com");
        assertThat(token.length()).isGreaterThan(100);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken("alice@example.com");
        String tampered = token.substring(0, token.length() - 4) + "abcd";
        assertThatThrownBy(() -> jwtService.extractEmail(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Date issued = new Date(System.currentTimeMillis() - 120_000);
        Date expired = new Date(System.currentTimeMillis() - 60_000);
        String token = jwtService.generateToken("alice@example.com", issued, expired);
        assertThatThrownBy(() -> jwtService.extractEmail(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void productionRefusesWeakSecret() {
        assertThatThrownBy(() -> new JwtService(
                "local-dev-only-secret-change-me-in-production-0123456789",
                1000,
                true
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> new JwtService("short", 1000, false))
                .isInstanceOf(IllegalStateException.class);
    }
}
