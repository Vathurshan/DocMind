package docmind.auth;

public record AuthResponse(
        String token,
        String email,
        String fullName
) {
}