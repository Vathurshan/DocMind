
package docmind.chat;

import java.util.UUID;

public record ChatSource(
        UUID documentId,
        String documentName,
        int chunkIndex,
        String content,
        int citationNumber
) {
}
