package docmind.embedding;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class EmbeddingClient {

    private final RestClient restClient;
    private final int expectedDimension;

    public EmbeddingClient(
            @Qualifier("embeddingRestClient") RestClient restClient,
            @Value("${app.embedding.dimension:384}") int expectedDimension
    ) {
        this.restClient = restClient;
        this.expectedDimension = expectedDimension;
    }

    public List<Double> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text to embed must not be blank");
        }

        EmbeddingResponse response;
        try {
            response = restClient.post()
                    .uri("/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new EmbeddingRequest(text))
                    .retrieve()
                    .body(EmbeddingResponse.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Embedding service request failed", e);
        }

        if (response == null || response.embedding() == null || response.embedding().isEmpty()) {
            throw new IllegalStateException("Embedding service returned an empty response");
        }

        List<Double> embedding = response.embedding();
        int dimensions = response.dimensions() > 0 ? response.dimensions() : embedding.size();
        if (dimensions != expectedDimension || embedding.size() != expectedDimension) {
            throw new IllegalStateException(
                    "Embedding has unexpected dimension: " + embedding.size()
                            + " (expected " + expectedDimension + ")"
            );
        }

        for (Double value : embedding) {
            if (value == null || value.isNaN() || value.isInfinite()) {
                throw new IllegalStateException("Embedding contains a non-finite value");
            }
        }

        return embedding;
    }

    public record EmbeddingRequest(String text) {
    }

    public record EmbeddingResponse(
            List<Double> embedding,
            int dimensions
    ) {
    }
}
