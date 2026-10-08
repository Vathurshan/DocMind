package docmind.document;

import docmind.embedding.EmbeddingClient;
import docmind.user.User;
import docmind.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingClient embeddingClient;

    private final Tika tika = new Tika();

    private final Path uploadDirectory =
            Paths.get("uploads");

    public Document uploadDocument(
            MultipartFile file,
            String userEmail
    ) throws IOException, TikaException {

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

        // Extract text using Apache Tika
        String extractedText =
                tika.parseToString(filePath.toFile());

        // Save document
        Document document = Document.builder()
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .extractedText(extractedText)
                .user(user)
                .build();

        Document savedDocument =
                documentRepository.save(document);

        // Split extracted text into chunks
        List<String> chunks =
                createChunks(extractedText, 1000);

        // Generate and save embedding for every chunk
        for (int i = 0; i < chunks.size(); i++) {

            String chunkText = chunks.get(i);

            List<Double> embedding =
                    embeddingClient.generateEmbedding(chunkText);

            float[] embeddingVector =
                    new float[embedding.size()];

            for (int j = 0; j < embedding.size(); j++) {
                embeddingVector[j] =
                        embedding.get(j).floatValue();
            }

            DocumentChunk documentChunk =
                    DocumentChunk.builder()
                            .document(savedDocument)
                            .chunkIndex(i)
                            .content(chunkText)
                            .embedding(embeddingVector)
                            .build();

            documentChunkRepository.save(documentChunk);
        }

        return savedDocument;
    }

    private List<String> createChunks(
            String text,
            int chunkSize
    ) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        String cleanedText =
                text.replaceAll("\\s+", " ").trim();

        java.util.ArrayList<String> chunks =
                new java.util.ArrayList<>();

        for (int start = 0;
             start < cleanedText.length();
             start += chunkSize) {

            int end =
                    Math.min(
                            start + chunkSize,
                            cleanedText.length()
                    );

            chunks.add(
                    cleanedText.substring(start, end)
            );
        }

        return chunks;
    }

    public List<DocumentResponse> getUserDocuments(
            String userEmail
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        return documentRepository
                .findByUserIdOrderByUploadedAtDesc(user.getId())
                .stream()
                .map(document ->
                        new DocumentResponse(
                                document.getId(),
                                document.getOriginalFileName(),
                                document.getContentType(),
                                document.getFileSize(),
                                document.getUploadedAt()
                        )
                )
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

        Document document =
                documentRepository
                        .findByIdAndUserId(
                                documentId,
                                user.getId()
                        )
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