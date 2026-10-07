package docmind.document;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public Document uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws Exception {

        return documentService.uploadDocument(
                file,
                authentication.getName()
        );
    }
    @GetMapping
    public java.util.List<DocumentResponse> getUserDocuments(
            Authentication authentication
    ) {
        return documentService.getUserDocuments(
                authentication.getName()
        );
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable UUID id,
            Authentication authentication
    ) throws Exception {

        documentService.deleteDocument(
                id,
                authentication.getName()
        );
    }
}