
package docmind.document;

import java.util.UUID;

public record HybridSearchResult(
        UUID documentId,
        String documentName,
        int chunkIndex,
        String content,
        Integer vectorRank,
        Integer keywordRank,
        double rrfScore
) {
}
