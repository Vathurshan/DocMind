package docmind.document;

import java.util.UUID;

/**
 * Native pgvector query projection. Aliases in SQL are quoted so PostgreSQL
 * preserves camelCase column names.
 */
public interface ChunkSearchHit {

    UUID getDocumentId();

    String getDocumentName();

    Integer getChunkIndex();

    String getContent();

    Double getDistance();
}
