package docmind.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    long countByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);

    /**
     * Cosine-distance nearest-neighbor search ({@code <=>} in pgvector).
     * Restricted to chunks that belong to the authenticated user's documents
     * and that actually have embeddings stored.
     */
    @Query(
            value = """
                    SELECT d.id AS "documentId",
                           d.original_file_name AS "documentName",
                           dc.chunk_index AS "chunkIndex",
                           dc.content AS "content",
                           (dc.embedding <=> CAST(:embedding AS vector)) AS "distance"
                    FROM document_chunks dc
                    JOIN documents d ON dc.document_id = d.id
                    WHERE d.user_id = :userId
                      AND dc.embedding IS NOT NULL
                    ORDER BY dc.embedding <=> CAST(:embedding AS vector)
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ChunkSearchHit> findSimilarChunksByUserId(
            @Param("userId") UUID userId,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );
}
