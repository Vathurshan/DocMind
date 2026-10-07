package docmind.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String originalFileName,
        String contentType,
        Long fileSize,
        Instant uploadedAt
) {
}