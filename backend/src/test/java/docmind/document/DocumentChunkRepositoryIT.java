package docmind.document;

import docmind.user.User;
import docmind.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentChunkRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("docmind")
            .withUsername("docmind")
            .withPassword("docmind");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.connection-init-sql", () -> "CREATE EXTENSION IF NOT EXISTS vector");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    UserRepository userRepository;
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    DocumentChunkRepository documentChunkRepository;

    @Test
    void similaritySearchIsScopedToOwnerAndOrderedByDistance() {
        User alice = userRepository.save(User.builder()
                .email("alice@example.com")
                .fullName("Alice")
                .passwordHash("hash")
                .build());
        User bob = userRepository.save(User.builder()
                .email("bob@example.com")
                .fullName("Bob")
                .passwordHash("hash")
                .build());

        Document aliceDoc = documentRepository.save(Document.builder()
                .originalFileName("security.pdf")
                .storedFileName("a.pdf")
                .contentType("application/pdf")
                .fileSize(10L)
                .extractedText("threats")
                .user(alice)
                .build());
        Document bobDoc = documentRepository.save(Document.builder()
                .originalFileName("other.pdf")
                .storedFileName("b.pdf")
                .contentType("application/pdf")
                .fileSize(10L)
                .extractedText("secret")
                .user(bob)
                .build());

        float[] close = vectorWith(0, 1.0f);
        float[] far = vectorWith(1, 1.0f);
        float[] bobVec = vectorWith(0, 1.0f);

        documentChunkRepository.save(DocumentChunk.builder()
                .document(aliceDoc)
                .chunkIndex(0)
                .content("A threat is a potential cause of an unwanted incident.")
                .embedding(close)
                .build());
        documentChunkRepository.save(DocumentChunk.builder()
                .document(aliceDoc)
                .chunkIndex(1)
                .content("The cafeteria menu is published on Fridays.")
                .embedding(far)
                .build());
        documentChunkRepository.save(DocumentChunk.builder()
                .document(aliceDoc)
                .chunkIndex(2)
                .content("This chunk has no embedding and must be ignored.")
                .embedding(null)
                .build());
        documentChunkRepository.save(DocumentChunk.builder()
                .document(bobDoc)
                .chunkIndex(0)
                .content("Bob's private notes about threats.")
                .embedding(bobVec)
                .build());

        String query = vectorLiteral(close);
        List<ChunkSearchHit> hits = documentChunkRepository.findSimilarChunksByUserId(
                alice.getId(),
                query,
                5
        );

        assertThat(hits).hasSize(2);
        assertThat(hits.getFirst().getContent()).contains("threat");
        assertThat(hits.getFirst().getDistance()).isLessThan(hits.get(1).getDistance());
        assertThat(hits).noneMatch(hit -> hit.getContent().contains("Bob"));
        assertThat(hits).noneMatch(hit -> hit.getContent().contains("no embedding"));
    }

    @Test
    void deletingDocumentRemovesChunks() {
        User alice = userRepository.save(User.builder()
                .email("alice-del@example.com")
                .fullName("Alice")
                .passwordHash("hash")
                .build());
        Document doc = documentRepository.save(Document.builder()
                .originalFileName("notes.txt")
                .storedFileName("n.txt")
                .contentType("text/plain")
                .fileSize(4L)
                .extractedText("hello")
                .user(alice)
                .build());
        documentChunkRepository.save(DocumentChunk.builder()
                .document(doc)
                .chunkIndex(0)
                .content("hello")
                .embedding(vectorWith(0, 1.0f))
                .build());

        documentRepository.delete(doc);
        documentRepository.flush();

        assertThat(documentChunkRepository.countByDocumentId(doc.getId())).isZero();
    }

    private static float[] vectorWith(int index, float value) {
        float[] vector = new float[384];
        vector[index] = value;
        return vector;
    }

    private static String vectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        return builder.append(']').toString();
    }
}
