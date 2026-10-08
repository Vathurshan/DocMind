package docmind.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor
public class EmbeddingTestController {

    private final EmbeddingClient embeddingClient;

    @PostMapping("/test")
    public String testEmbedding(@RequestBody String text) {

        List<Double> embedding =
                embeddingClient.generateEmbedding(text);

        return "Embedding generated successfully. Dimensions: "
                + embedding.size();
    }
}