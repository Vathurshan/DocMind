package docmind.document;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return documentService.uploadDocument(file, authentication.getName());
    }

    @GetMapping
    public List<DocumentResponse> getUserDocuments(Authentication authentication) {
        return documentService.getUserDocuments(authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        documentService.deleteDocument(id, authentication.getName());
    }

    @PostMapping("/search")
    public List<SearchResult> searchDocuments(
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
