package docmind.document;

import docmind.user.User;
import docmind.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    private final Path uploadDirectory =
            Paths.get("uploads");

    public Document uploadDocument(
            MultipartFile file,
            String userEmail
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        Files.createDirectories(uploadDirectory);

        String storedFileName =
                UUID.randomUUID() + "_" + file.getOriginalFilename();

        Path filePath =
                uploadDirectory.resolve(storedFileName);

        Files.copy(
                file.getInputStream(),
                filePath
        );

        Document document = Document.builder()
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .user(user)
                .build();

        return documentRepository.save(document);
    }
    public java.util.List<DocumentResponse> getUserDocuments(
            String userEmail
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        return documentRepository
                .findByUserIdOrderByUploadedAtDesc(user.getId())
                .stream()
                .map(document -> new DocumentResponse(
                        document.getId(),
                        document.getOriginalFileName(),
                        document.getContentType(),
                        document.getFileSize(),
                        document.getUploadedAt()
                ))
                .toList();
    }
    public void deleteDocument(
            UUID documentId,
            String userEmail
    ) throws IOException {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        Document document = documentRepository
                .findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found"
                        )
                );

        Path filePath =
                uploadDirectory.resolve(
                        document.getStoredFileName()
                );

        Files.deleteIfExists(filePath);

        documentRepository.delete(document);
    }
}