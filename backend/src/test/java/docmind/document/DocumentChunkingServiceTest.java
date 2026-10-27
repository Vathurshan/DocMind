package docmind.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentChunkingServiceTest {

    private final DocumentChunkingService service = new DocumentChunkingService();

    @Test
    void emptyAndBlankTextYieldsNoChunks() {
        assertThat(service.split(null)).isEmpty();
        assertThat(service.split("")).isEmpty();
        assertThat(service.split("   \n\t  ")).isEmpty();
    }

    @Test
    void shortTextIsASingleChunk() {
        assertThat(service.split("hello world")).containsExactly("hello world");
    }

    @Test
    void longTextSplitsOnChunkSizeWithoutOverlap() {
        String text = "a".repeat(2500);
        List<String> chunks = service.split(text, 1000);
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(1000);
        assertThat(chunks.get(1)).hasSize(1000);
        assertThat(chunks.get(2)).hasSize(500);
        assertThat(chunks.get(0).charAt(0)).isEqualTo('a');
    }

    @Test
    void whitespaceIsNormalized() {
        List<String> chunks = service.split("hello   \n\n  world");
        assertThat(chunks).containsExactly("hello world");
    }

    @Test
    void indexesAreStableForExactBoundary() {
        String text = "x".repeat(1000);
        assertThat(service.split(text, 1000)).containsExactly(text);
    }

    @Test
    void rejectsNonPositiveChunkSize() {
        assertThatThrownBy(() -> service.split("abc", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
