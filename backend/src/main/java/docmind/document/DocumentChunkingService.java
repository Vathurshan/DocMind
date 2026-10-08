package docmind.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentChunkingService {

    private final DocumentChunkRepository documentChunkRepository;

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    public List<DocumentChunk> createChunks(
            Document document
    ) {

        String text = document.getExtractedText();

        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<DocumentChunk> chunks = new ArrayList<>();

        int start = 0;
        int chunkIndex = 0;

        while (start < text.length()) {

            int end = Math.min(
                    start + CHUNK_SIZE,
                    text.length()
            );

            String chunkText = text
                    .substring(start, end)
                    .trim();

            if (!chunkText.isBlank()) {

                DocumentChunk chunk = DocumentChunk.builder()
                        .document(document)
                        .chunkIndex(chunkIndex)
                        .content(chunkText)
                        .build();

                chunks.add(chunk);

                chunkIndex++;
            }

            if (end == text.length()) {
                break;
            }

            start = end - CHUNK_OVERLAP;
        }

        return documentChunkRepository.saveAll(chunks);
    }
}