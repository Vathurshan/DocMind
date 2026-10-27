package docmind.document;

import docmind.embedding.EmbeddingClient;
import docmind.user.User;
import docmind.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "text/markdown",
            "text/x-markdown"
    );

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingClient embeddingClient;
    private final DocumentChunkingService documentChunkingService;
    private final PlatformTransactionManager transactionManager;

    private final Tika tika = new Tika();

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public DocumentResponse uploadDocument(MultipartFile file, String userEmail) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty or missing");
        }

        User user = findUser(userEmail);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }
        if (bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty or missing");
        }

        String originalName = sanitizeOriginalFileName(file.getOriginalFilename());
        String detectedType = tika.detect(bytes, originalName);
        if (!isAllowedType(detectedType, originalName)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file type"
            );
        }

        String extractedText;
        try {
            extractedText = tika.parseToString(new ByteArrayInputStream(bytes)).trim();
        } catch (Exception e) {
            log.warn("Text extraction failed for {}: {}", originalName, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Failed to extract text from document"
            );
        }

        List<String> chunkTexts = documentChunkingService.split(extractedText);
        List<float[]> embeddings = new ArrayList<>(chunkTexts.size());
        for (String chunkText : chunkTexts) {
            embeddings.add(toVector(embedOrFail(chunkText)));
        }

        Path storedPath = null;
        try {
            Path uploadDirectory = uploadDirectory();
            Files.createDirectories(uploadDirectory);
            String storedFileName = UUID.randomUUID() + "_" + originalName;
            storedPath = safeResolve(uploadDirectory, storedFileName);
            Files.write(storedPath, bytes);

            Path committedPath = storedPath;
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            Document saved = tx.execute(status -> {
                Document document = Document.builder()
                        .originalFileName(originalName)
                        .storedFileName(committedPath.getFileName().toString())
                        .contentType(detectedType)
                        .fileSize((long) bytes.length)
                        .extractedText(extractedText)
                        .user(user)
                        .build();
                Document persisted = documentRepository.save(document);

                for (int i = 0; i < chunkTexts.size(); i++) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .document(persisted)
                            .chunkIndex(i)
                            .content(chunkTexts.get(i))
                            .embedding(embeddings.get(i))
                            .build();
                    persisted.getChunks().add(chunk);
                    documentChunkRepository.save(chunk);
                }
                return persisted;
            });

            if (saved == null) {
                throw new IllegalStateException("Document was not persisted");
            }

            log.info(
                    "Uploaded document '{}' for user '{}' with {} chunks",
                    originalName,
                    userEmail,
                    chunkTexts.size()
            );
            return toResponse(saved);
        } catch (RuntimeException e) {
            deleteQuietly(storedPath);
            throw e;
        } catch (IOException e) {
            deleteQuietly(storedPath);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store uploaded file"
            );
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getUserDocuments(String userEmail) {
        User user = findUser(userEmail);
        return documentRepository
                .findByUserIdOrderByUploadedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteDocument(UUID documentId, String userEmail) {
        User user = findUser(userEmail);
        Document document = documentRepository
                .findByIdAndUserId(documentId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found or access denied"
                ));

        Path filePath = safeResolve(uploadDirectory(), document.getStoredFileName());
        documentChunkRepository.deleteByDocumentId(document.getId());
        documentRepository.delete(document);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(filePath);
            }
        });

        log.info("Deleted document '{}' for user '{}'", document.getOriginalFileName(), userEmail);
    }

    @Transactional(readOnly = true)
    public List<SearchResult> searchDocuments(String query, String userEmail, int limit) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query must not be blank");
        }

        User user = findUser(userEmail);
        List<Double> queryEmbedding = embedOrFail(query);
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        int safeLimit = Math.min(Math.max(limit, 1), 20);

        return documentChunkRepository
                .findSimilarChunksByUserId(user.getId(), vectorLiteral, safeLimit)
                .stream()
                .map(hit -> new SearchResult(
                        hit.getDocumentId(),
                        hit.getDocumentName(),
                        hit.getChunkIndex(),
                        hit.getContent(),
                        hit.getDistance() == null ? 0.0 : hit.getDistance()
                ))
                .toList();
    }

    private List<Double> embedOrFail(String text) {
        try {
            return embeddingClient.generateEmbedding(text);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding service error: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Embedding service is unavailable. Please ensure the Python service is running."
            );
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"
                ));
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getUploadedAt()
        );
    }

    private Path uploadDirectory() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private Path safeResolve(Path directory, String fileName) {
        Path resolved = directory.resolve(fileName).normalize();
        if (!resolved.startsWith(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stored file name");
        }
        return resolved;
    }

    static String sanitizeOriginalFileName(String original) {
        if (original == null || original.isBlank()) {
            return "document";
        }
        String name = Paths.get(original.replace("\\", "/")).getFileName().toString();
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            return "document";
        }
        return name.length() > 180 ? name.substring(name.length() - 180) : name;
    }

    private boolean isAllowedType(String detectedType, String originalName) {
        String type = detectedType == null ? "" : detectedType.toLowerCase(Locale.ROOT);
        if (ALLOWED_MEDIA_TYPES.contains(type)) {
            return true;
        }
        String lower = originalName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") && type.startsWith("text/")
                || lower.endsWith(".md") && type.startsWith("text/");
    }

    private static float[] toVector(List<Double> embedding) {
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i).floatValue();
        }
        return vector;
    }

    private static String toVectorLiteral(List<Double> embedding) {
        return embedding.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", path, e.getMessage());
        }
    }
}
