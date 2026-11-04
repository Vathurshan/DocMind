package docmind.document;

import java.util.UUID;

public interface KeywordChunkSearchHit {

    UUID getDocumentId();

    String getDocumentName();

    Integer getChunkIndex();

    String getContent();

    Double getKeywordRank();
}