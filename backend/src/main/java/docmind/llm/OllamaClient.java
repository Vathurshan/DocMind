package docmind.llm;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Component
public class OllamaClient {

    private final RestClient restClient;

    public OllamaClient(
            @Qualifier("ollamaRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    @Value("${app.ollama.model:qwen2.5:3b}")
    private String model;

    public String generateAnswer(String systemPrompt, String userPrompt) {

        OllamaRequest request = new OllamaRequest(
                model,
                List.of(
                        new OllamaMessage("system", systemPrompt),
                        new OllamaMessage("user", userPrompt)
                ),
                false,
                Map.of("temperature", 0.2)
        );

        try {
            OllamaResponse response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OllamaResponse.class);

            if (response == null
                    || response.message() == null
                    || response.message().content() == null
                    || response.message().content().isBlank()) {
                throw new IllegalStateException(
                        "Ollama returned an empty response"
                );
            }

            return response.message().content().trim();

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Local AI service is unavailable. Please ensure Ollama is running.",
                    e
            );
        }
    }

    public record OllamaRequest(
            String model,
            List<OllamaMessage> messages,
            boolean stream,
            Map<String, Object> options
    ) {
    }

    public record OllamaMessage(
            String role,
            String content
    ) {
    }

    public record OllamaResponse(
            OllamaMessage message
    ) {
    }
}