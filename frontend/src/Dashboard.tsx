import { useEffect, useRef, useState } from "react";
import { ApiError, api } from "./api";
import "./Dashboard.css";

// ─── Types ────────────────────────────────────────────────────────────────────

interface DocumentItem {
  id: string;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  uploadedAt: string;
}

interface SearchResultItem {
  documentId: string;
  documentName: string;
  chunkIndex: number;
  content: string;
  distance: number;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
}

function userInitial() {
  const name = localStorage.getItem("fullName") || "";
  return name.charAt(0).toUpperCase() || "U";
}

function userName() {
  return localStorage.getItem("fullName") || "User";
}

// ─── Component ────────────────────────────────────────────────────────────────

function Dashboard() {
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Document state
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [isLoadingDocs, setIsLoadingDocs] = useState(true);
  const [docsError, setDocsError] = useState("");

  // Upload state
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");

  // Search state
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<SearchResultItem[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [hasSearched, setHasSearched] = useState(false);

  // Delete state
  const [deletingId, setDeletingId] = useState<string | null>(null);

  // Active section
  const [activeSection, setActiveSection] = useState<"dashboard" | "search">("dashboard");

  // ── Fetch documents on mount ──────────────────────────────────────────────

  useEffect(() => {
    fetchDocuments();
  }, []);

  const fetchDocuments = async () => {
    setIsLoadingDocs(true);
    setDocsError("");
    try {
      const data = await api.get<DocumentItem[]>("/api/documents");
      setDocuments(data);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        window.location.reload();
        return;
      }
      setDocsError("Could not load your documents. Please refresh.");
    } finally {
      setIsLoadingDocs(false);
    }
  };

  // ── Upload ────────────────────────────────────────────────────────────────

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Reset input so the same file can be re-uploaded
    event.target.value = "";

    setUploadError("");
    setIsUploading(true);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const uploaded = await api.postForm<DocumentItem>("/api/documents/upload", formData);
      setDocuments((prev) => [uploaded, ...prev]);
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        window.location.reload();
        return;
      }
      const msg = err instanceof Error ? err.message : "Upload failed";
      setUploadError(msg);
    } finally {
      setIsUploading(false);
    }
  };

  // ── Delete ────────────────────────────────────────────────────────────────

  const handleDelete = async (id: string, name: string) => {
    if (!window.confirm(`Delete "${name}"? This cannot be undone.`)) return;

    setDeletingId(id);
    try {
      await api.delete(`/api/documents/${id}`);
      setDocuments((prev) => prev.filter((d) => d.id !== id));
      // Clear search results that reference deleted doc
      setSearchResults((prev) => prev.filter((r) => r.documentId !== id));
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        window.location.reload();
        return;
      }
      const msg = err instanceof Error ? err.message : "Delete failed";
      alert(msg);
    } finally {
      setDeletingId(null);
    }
  };

  // ── Search ────────────────────────────────────────────────────────────────

  const handleSearch = async () => {
    const query = searchQuery.trim();
    if (!query) {
      setSearchError("Please enter a search query.");
      return;
    }

    setSearchError("");
    setIsSearching(true);
    setHasSearched(false);
    setSearchResults([]);

    try {
      const results = await api.postJson<SearchResultItem[]>("/api/search", {
        query,
        limit: 10,
      });
      setSearchResults(results);
      setHasSearched(true);
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        window.location.reload();
        return;
      }
      const msg = err instanceof Error ? err.message : "Search failed";
      setSearchError(msg);
    } finally {
      setIsSearching(false);
    }
  };

  // ── Logout ────────────────────────────────────────────────────────────────

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("fullName");
    localStorage.removeItem("email");
    window.location.reload();
  };

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <div className="dashboard">

      {/* SIDEBAR */}
      <aside className="sidebar">

        <div className="sidebar-logo">
          <div className="sidebar-logo-icon">✦</div>
          <span>Doc<span>Mind</span></span>
        </div>

        <nav className="sidebar-nav">

          <button
            className={`nav-item ${activeSection === "dashboard" ? "active" : ""}`}
            onClick={() => setActiveSection("dashboard")}
          >
            <span>⌂</span>
            Dashboard
          </button>

          <button
            className={`nav-item ${activeSection === "search" ? "active" : ""}`}
            onClick={() => setActiveSection("search")}
          >
            <span>🔍</span>
            Semantic Search
          </button>

        </nav>

        <div className="sidebar-bottom">

          <div className="sidebar-user">
            <div className="sidebar-user-avatar">{userInitial()}</div>
            <div className="sidebar-user-info">
              <span className="sidebar-user-name">{userName()}</span>
              <span className="sidebar-user-email">{localStorage.getItem("email") || ""}</span>
            </div>
          </div>

          <button className="logout-button" onClick={handleLogout}>
            <span>↪</span>
            Logout
          </button>

        </div>

      </aside>


      {/* MAIN CONTENT */}
      <main className="dashboard-main">

        {/* ── DASHBOARD SECTION ── */}
        {activeSection === "dashboard" && (
          <>
            <header className="dashboard-header">
              <div>
                <p className="welcome-small">Welcome back 👋</p>
                <h1>Your Document Workspace</h1>
                <p className="dashboard-subtitle">
                  Upload documents and let DocMind help you understand them.
                </p>
              </div>
              <div className="user-avatar">{userInitial()}</div>
            </header>


            {/* UPLOAD CARD */}
            <section className="upload-card">

              <div className="upload-icon">↑</div>

              <h2>Upload a document</h2>
              <p>Add a PDF, DOCX, or TXT file and start asking questions about it.</p>

              {isUploading ? (
                <div className="uploading-state">
                  <div className="upload-spinner"></div>
                  <span>Uploading &amp; processing embeddings…</span>
                </div>
              ) : (
                <button
                  className="upload-button"
                  onClick={() => fileInputRef.current?.click()}
                >
                  + Upload Document
                </button>
              )}

              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.docx,.txt,.doc,.pptx,.xlsx,.md"
                style={{ display: "none" }}
                onChange={handleFileChange}
                disabled={isUploading}
              />

              <span className="upload-hint">Supported: PDF, DOCX, TXT, PPTX, XLSX, MD</span>

              {uploadError && (
                <p className="inline-error">{uploadError}</p>
              )}

            </section>


            {/* STATS */}
            <section className="stats-grid">

              <div className="stat-card">
                <div className="stat-icon">📄</div>
                <div>
                  <span>Documents</span>
                  <strong>{documents.length}</strong>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">🔍</div>
                <div>
                  <span>Semantic Search</span>
                  <strong>Ready</strong>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">✦</div>
                <div>
                  <span>AI Powered</span>
                  <strong>pgvector</strong>
                </div>
              </div>

            </section>


            {/* DOCUMENTS LIST */}
            <section className="recent-section">

              <div className="section-heading">
                <div>
                  <h2>Recent Documents</h2>
                  <p>Your recently uploaded documents will appear here.</p>
                </div>
                <button className="view-all" onClick={fetchDocuments}>
                  ↻ Refresh
                </button>
              </div>

              {docsError && <p className="inline-error">{docsError}</p>}

              {isLoadingDocs ? (
                <div className="loading-state">
                  <div className="upload-spinner"></div>
                  <span>Loading documents…</span>
                </div>
              ) : documents.length === 0 ? (
                <div className="empty-documents">
                  <div className="empty-icon">📂</div>
                  <h3>No documents yet</h3>
                  <p>Upload your first document to get started with DocMind.</p>
                </div>
              ) : (
                <div className="document-list">
                  {documents.map((doc) => (
                    <div className="document-item" key={doc.id}>

                      <div className="document-icon">📄</div>

                      <div className="document-info">
                        <h3>{doc.originalFileName}</h3>
                        <p>{formatBytes(doc.fileSize)} · {doc.contentType}</p>
                      </div>

                      <div className="document-date">
                        {new Date(doc.uploadedAt).toLocaleDateString()}
                      </div>

                      <button
                        className="delete-button"
                        onClick={() => handleDelete(doc.id, doc.originalFileName)}
                        disabled={deletingId === doc.id}
                      >
                        {deletingId === doc.id ? "Deleting…" : "Delete"}
                      </button>

                    </div>
                  ))}
                </div>
              )}

            </section>
          </>
        )}


        {/* ── SEARCH SECTION ── */}
        {activeSection === "search" && (
          <>
            <header className="dashboard-header">
              <div>
                <p className="welcome-small">AI-Powered ✦</p>
                <h1>Semantic Search</h1>
                <p className="dashboard-subtitle">
                  Ask a natural-language question across all your documents.
                </p>
              </div>
              <div className="user-avatar">{userInitial()}</div>
            </header>


            {/* SEARCH BOX */}
            <section className="search-card">

              <div className="search-input-row">
                <input
                  id="search-input"
                  type="text"
                  className="search-input"
                  placeholder="What does the document explain about machine learning?"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                  disabled={isSearching}
                />
                <button
                  id="search-button"
                  className="search-button"
                  onClick={handleSearch}
                  disabled={isSearching || !searchQuery.trim()}
                >
                  {isSearching ? (
                    <>
                      <div className="btn-spinner"></div>
                      Searching…
                    </>
                  ) : (
                    <>🔍 Search</>
                  )}
                </button>
              </div>

              {searchError && <p className="inline-error">{searchError}</p>}

              <p className="search-hint">
                Press Enter or click Search · Results ranked by semantic similarity
              </p>

            </section>


            {/* SEARCH RESULTS */}
            {hasSearched && (
              <section className="results-section">

                <div className="section-heading">
                  <div>
                    <h2>Search Results</h2>
                    <p>
                      {searchResults.length === 0
                        ? "No matching chunks found."
                        : `Found ${searchResults.length} relevant passage${searchResults.length === 1 ? "" : "s"}`}
                    </p>
                  </div>
                </div>

                {searchResults.length === 0 ? (
                  <div className="empty-documents">
                    <div className="empty-icon">🔍</div>
                    <h3>No results found</h3>
                    <p>Try a different query or upload more documents first.</p>
                  </div>
                ) : (
                  <div className="results-list">
                    {searchResults.map((result, i) => (
                      <div className="result-item" key={`${result.documentId}-${result.chunkIndex}-${i}`}>

                        <div className="result-rank">#{i + 1}</div>

                        <div className="result-body">

                          <div className="result-source">
                            <span className="result-doc-icon">📄</span>
                            <span className="result-doc-name">{result.documentName}</span>
                            <span className="result-chunk-badge">chunk {result.chunkIndex + 1}</span>
                          </div>

                          <p className="result-content">{result.content}</p>

                        </div>

                      </div>
                    ))}
                  </div>
                )}

              </section>
            )}

            {/* Prompt to upload if no docs */}
            {!isLoadingDocs && documents.length === 0 && !hasSearched && (
              <div className="search-empty-hint">
                <div className="empty-icon">📂</div>
                <h3>No documents yet</h3>
                <p>
                  Go to <button className="link-button" onClick={() => setActiveSection("dashboard")}>Dashboard</button> to upload documents first.
                </p>
              </div>
            )}

          </>
        )}

      </main>

    </div>
  );
}

export default Dashboard;