package docmind.embedding;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingClientTest {

    private MockWebServer server;
    private EmbeddingClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        client = new EmbeddingClient(restClient, 384);
    }

    @AfterEach
    void shutdown() throws IOException {
        server.shutdown();
    }

    @Test
    void returnsValidatedEmbedding() {
        String values = IntStream.range(0, 384)
                .mapToObj(i -> "0.1")
                .collect(Collectors.joining(","));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"embedding\":[" + values + "],\"dimensions\":384}"));

        List<Double> embedding = client.generateEmbedding("hello");
        assertThat(embedding).hasSize(384);
        assertThat(embedding.getFirst()).isEqualTo(0.1);
    }

    @Test
    void rejectsWrongDimension() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"embedding\":[0.1,0.2],\"dimensions\":2}"));

        assertThatThrownBy(() -> client.generateEmbedding("hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dimension");
    }

    @Test
    void rejectsNonFiniteValues() {
        String values = IntStream.range(0, 383)
                .mapToObj(i -> "0.1")
                .collect(Collectors.joining(","));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"embedding\":[" + values + ",null],\"dimensions\":384}"));

        assertThatThrownBy(() -> client.generateEmbedding("hello"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsBlankTextWithoutCallingService() {
        assertThatThrownBy(() -> client.generateEmbedding("  "))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void reportsHttpFailures() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
        assertThatThrownBy(() -> client.generateEmbedding("hello"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Embedding service");
    }
}
