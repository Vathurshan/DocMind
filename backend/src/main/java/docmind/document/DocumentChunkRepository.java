
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
     * Semantic retrieval using pgvector cosine distance.
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

    /**
     * Keyword retrieval using PostgreSQL full-text search.
     */
    @Query(
            value = """
                SELECT d.id AS "documentId",
                       d.original_file_name AS "documentName",
                       dc.chunk_index AS "chunkIndex",
                       dc.content AS "content",
                       ts_rank_cd(
                           dc.search_vector,
                           to_tsquery('english', :query)
                       ) AS "keywordRank"
                FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
                WHERE d.user_id = :userId
                  AND dc.search_vector @@ to_tsquery('english', :query)
                ORDER BY ts_rank_cd(
                    dc.search_vector,
                    to_tsquery('english', :query)
                ) DESC
                LIMIT :limit
                """,
            nativeQuery = true
    )
    List<KeywordChunkSearchHit> findKeywordChunksByUserId(
            @Param("userId") UUID userId,
            @Param("query") String query,
            @Param("limit") int limit
    );

}
