package docmind.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("http://127.0.0.1:8000")
            .build();

    public List<Double> generateEmbedding(String text) {

        EmbeddingRequest request = new EmbeddingRequest(text);

        EmbeddingResponse response = restClient.post()
                .uri("/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.embedding() == null) {
            throw new IllegalStateException(
                    "Embedding service returned an empty response"
            );
        }

        return response.embedding();
    }

    private record EmbeddingRequest(String text) {
    }

    private record EmbeddingResponse(
            List<Double> embedding,
            int dimensions
    ) {
    }
}