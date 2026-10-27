package docmind.document;

import docmind.embedding.EmbeddingClient;
import docmind.user.User;
import docmind.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    DocumentRepository documentRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    DocumentChunkRepository documentChunkRepository;
    @Mock
    EmbeddingClient embeddingClient;
    @Mock
    PlatformTransactionManager transactionManager;

    DocumentService documentService;
    User owner;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository,
                userRepository,
                documentChunkRepository,
                embeddingClient,
                new DocumentChunkingService(),
                transactionManager
        );
        owner = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .fullName("Alice")
                .passwordHash("hash")
                .build();
    }

    @Test
    void emptyFileIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        assertThatThrownBy(() -> documentService.uploadDocument(file, "alice@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void blankSearchQueryIsRejected() {
        assertThatThrownBy(() -> documentService.searchDocuments("  ", "alice@example.com", 5))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deleteMissingDocumentIsNotFound() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(owner));
        when(documentRepository.findByIdAndUserId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(owner.getId())))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deleteDocument(UUID.randomUUID(), "alice@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void sanitizeStripsPathTraversal() {
        assertThat(DocumentService.sanitizeOriginalFileName("../etc/passwd"))
                .isEqualTo("passwd");
        assertThat(DocumentService.sanitizeOriginalFileName("C:\\\\temp\\\\notes.pdf"))
                .isEqualTo("notes.pdf");
    }

    @Test
    void searchMapsHitsOwnedByUser() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(owner));
        when(embeddingClient.generateEmbedding("threat")).thenReturn(zeros(384));

        UUID documentId = UUID.randomUUID();
        ChunkSearchHit hit = new ChunkSearchHit() {
            @Override
            public UUID getDocumentId() {
                return documentId;
            }

            @Override
            public String getDocumentName() {
                return "guide.pdf";
            }

            @Override
            public Integer getChunkIndex() {
                return 2;
            }

            @Override
            public String getContent() {
                return "A threat is a potential cause of harm.";
            }

            @Override
            public Double getDistance() {
                return 0.324;
            }
        };
        when(documentChunkRepository.findSimilarChunksByUserId(
                org.mockito.ArgumentMatchers.eq(owner.getId()),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(5)
        )).thenReturn(List.of(hit));

        List<SearchResult> results = documentService.searchDocuments("threat", "alice@example.com", 5);
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().documentName()).isEqualTo("guide.pdf");
        assertThat(results.getFirst().distance()).isEqualTo(0.324);
        assertThat(results.getFirst().documentId()).isEqualTo(documentId);
    }

    private static List<Double> zeros(int n) {
        return java.util.Collections.nCopies(n, 0.0);
    }
}
