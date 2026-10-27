package docmind.document;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits extracted document text into ordered, non-empty chunks.
 * Overlap is not used: the established pipeline stores 1000-character windows.
 */
@Service
public class DocumentChunkingService {

    public static final int DEFAULT_CHUNK_SIZE = 1000;

    public List<String> split(String text) {
        return split(text, DEFAULT_CHUNK_SIZE);
    }

    public List<String> split(String text, int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String cleanedText = text.replaceAll("\\s+", " ").trim();
        if (cleanedText.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < cleanedText.length(); start += chunkSize) {
            int end = Math.min(start + chunkSize, cleanedText.length());
            String chunk = cleanedText.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }
}
