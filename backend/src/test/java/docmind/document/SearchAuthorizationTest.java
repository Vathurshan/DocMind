package docmind.document;

import docmind.security.JwtAuthFilter;
import docmind.security.JwtService;
import docmind.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SearchController.class, DocumentController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class})
class SearchAuthorizationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @MockitoBean
    DocumentService documentService;

    @Test
    void postApiSearchWithValidJwtReturnsResults() throws Exception {
        when(documentService.searchDocuments(eq("What is a security threat?"), eq("alice@example.com"), anyInt()))
                .thenReturn(List.of(new SearchResult(
                        UUID.randomUUID(),
                        "notes.pdf",
                        0,
                        "A threat is a potential cause of an unwanted incident.",
                        0.32
                )));

        mockMvc.perform(post("/api/search")
                        .header("Authorization", "Bearer " + jwtService.generateToken("alice@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"What is a security threat?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentName").value("notes.pdf"))
                .andExpect(jsonPath("$[0].content").exists());

        verify(documentService).searchDocuments("What is a security threat?", "alice@example.com", 10);
    }

    @Test
    void postApiDocumentsSearchWithValidJwtReturnsOk() throws Exception {
        when(documentService.searchDocuments(anyString(), anyString(), anyInt())).thenReturn(List.of());

        mockMvc.perform(post("/api/documents/search")
                        .header("Authorization", "Bearer " + jwtService.generateToken("alice@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"routing","limit":5}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void getDocumentsWithSameJwtReturnsOk() throws Exception {
        when(documentService.getUserDocuments("alice@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + jwtService.generateToken("alice@example.com")))
                .andExpect(status().isOk());
    }

    @Test
    void postApiSearchWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"What is a security threat?"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postApiSearchWithInvalidJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/search")
                        .header("Authorization", "Bearer not-a-real-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"What is a security threat?"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postApiSearchWithExpiredJwtIsUnauthorized() throws Exception {
        Date issued = new Date(System.currentTimeMillis() - 120_000);
        Date expired = new Date(System.currentTimeMillis() - 1_000);
        String token = jwtService.generateToken("alice@example.com", issued, expired);

        mockMvc.perform(post("/api/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"What is a security threat?"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
