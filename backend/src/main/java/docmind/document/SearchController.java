package docmind.document;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final DocumentService documentService;

    @PostMapping
    public List<SearchResult> search(
            @Valid @RequestBody SearchRequest request,
            Authentication authentication
    ) {
        return documentService.searchDocuments(
                request.query(),
                authentication.getName(),
                request.limit()
        );
    }
}
