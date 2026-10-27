package docmind.document;

import java.util.UUID;

public record SearchResult(
        UUID documentId,
        String documentName,
        int chunkIndex,
        String content,
        double distance
) {
}
