import { useEffect, useRef, useState } from "react";
import "./Dashboard.css";

function Dashboard() {

  const fileInputRef = useRef<HTMLInputElement>(null);

  const [documents, setDocuments] = useState<any[]>([]);
  useEffect(() => {
  const fetchDocuments = async () => {

    const token = localStorage.getItem("token");

    if (!token) {
      return;
    }

    try {

      const response = await fetch(
        "http://localhost:8080/api/documents",
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        console.error(
          "Failed to fetch documents:",
          response.status
        );
        return;
      }

      const data = await response.json();

      setDocuments(data);

      console.log("Documents:", data);

    } catch (error) {

      console.error(
        "Error fetching documents:",
        error
      );

    }
  };

  fetchDocuments();

}, []);
  const handleFileChange = async (
    
  event: React.ChangeEvent<HTMLInputElement>
) => {

  const file = event.target.files?.[0];

  if (!file) {
    return;
  }

  const token = localStorage.getItem("token");

  if (!token) {
    alert("You are not logged in.");
    return;
  }

  const formData = new FormData();

  formData.append("file", file);

  try {

    const response = await fetch(
      "http://localhost:8080/api/documents/upload",
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      }
    );

    if (!response.ok) {
  const errorText = await response.text();
  console.error("Server response:", errorText);

  throw new Error(
    `Upload failed (${response.status}): ${errorText}`
  );
}

const uploadedDocument = await response.json();

console.log("Uploaded document:", uploadedDocument);

setDocuments((previousDocuments) => [
  uploadedDocument,
  ...previousDocuments,
]);

alert("Document uploaded successfully!");

  } catch (error) {

    console.error("Upload error:", error);

    alert("Failed to upload document.");

  }
};
const handleDeleteDocument = async (documentId: string) => {

  const token = localStorage.getItem("token");

  if (!token) {
    alert("You are not logged in.");
    return;
  }

  const confirmed = window.confirm(
    "Are you sure you want to delete this document?"
  );

  if (!confirmed) {
    return;
  }

  try {

    const response = await fetch(
      `http://localhost:8080/api/documents/${documentId}`,
      {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {

      const errorText = await response.text();

      console.error(
        "Delete failed:",
        errorText
      );

      throw new Error(
        `Delete failed (${response.status})`
      );
    }

    setDocuments((previousDocuments) =>
      previousDocuments.filter(
        (document) => document.id !== documentId
      )
    );

    alert("Document deleted successfully!");

  } catch (error) {

    console.error(
      "Delete error:",
      error
    );

    alert("Failed to delete document.");
  }
};

  const handleLogout = () => {
    localStorage.removeItem("token");
    window.location.reload();
  };

  return (
    <div className="dashboard">

      {/* SIDEBAR */}
      <aside className="sidebar">

        <div className="sidebar-logo">
          <div className="sidebar-logo-icon">✦</div>
          <span>Doc<span>Mind</span></span>
        </div>

        <nav className="sidebar-nav">

          <button className="nav-item active">
            <span>⌂</span>
            Dashboard
          </button>

          <button className="nav-item">
            <span>📄</span>
            My Documents
          </button>

          <button className="nav-item">
            <span>💬</span>
            AI Chat
          </button>

        </nav>

        <div className="sidebar-bottom">

          <button className="nav-item">
            <span>⚙</span>
            Settings
          </button>

          <button
            className="logout-button"
            onClick={handleLogout}
>
            <span>↪</span>
             Logout
         </button>

        </div>

      </aside>


      {/* MAIN CONTENT */}
      <main className="dashboard-main">

        <header className="dashboard-header">

          <div>
            <p className="welcome-small">
              Welcome back 👋
            </p>

            <h1>
              Your Document Workspace
            </h1>

            <p className="dashboard-subtitle">
              Upload documents and let DocMind help you understand them.
            </p>
          </div>

          <div className="user-avatar">
            V
          </div>

        </header>


        {/* UPLOAD CARD */}
        <section className="upload-card">

          <div className="upload-icon">
            ↑
          </div>

          <h2>
            Upload a document
          </h2>

          <p>
            Add a PDF or document and start asking questions about it.
          </p>

          <button
  className="upload-button"
  onClick={() => fileInputRef.current?.click()}
>
  + Upload Document
</button>

<input
  ref={fileInputRef}
  type="file"
  accept=".pdf,.docx,.txt"
  style={{ display: "none" }}
  onChange={handleFileChange}
/>

          <span className="upload-hint">
            Supported formats: PDF, DOCX, TXT
          </span>

        </section>


        {/* STATS */}
        <section className="stats-grid">

          <div className="stat-card">
            <div className="stat-icon">📄</div>
            <div>
              <span>Documents</span>
              <strong>0</strong>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">💬</div>
            <div>
              <span>Questions Asked</span>
              <strong>0</strong>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">✦</div>
            <div>
              <span>AI Answers</span>
              <strong>0</strong>
            </div>
          </div>

        </section>


        {/* RECENT DOCUMENTS */}
        <section className="recent-section">

          <div className="section-heading">
            <div>
              <h2>Recent Documents</h2>
              <p>Your recently uploaded documents will appear here.</p>
            </div>

            <button className="view-all">
              View all →
            </button>
          </div>

          {documents.length === 0 ? (

  <div className="empty-documents">

    <div className="empty-icon">
      📂
    </div>

    <h3>
      No documents yet
    </h3>

    <p>
      Upload your first document to get started with DocMind.
    </p>

  </div>

) : (

  <div className="document-list">

    {documents.map((document) => (

      <div
        className="document-item"
        key={document.id}
      >

        <div className="document-icon">
          📄
        </div>

        <div className="document-info">

          <h3>
            {document.originalFileName}
          </h3>

          <p>
            {(document.fileSize / 1024 / 1024).toFixed(2)} MB
          </p>

        </div>

<div className="document-date">

  {new Date(
    document.uploadedAt
  ).toLocaleDateString()}

</div>

<button
  className="delete-button"
  onClick={() =>
    handleDeleteDocument(document.id)
  }
>
  Delete
</button>

      </div>

    ))}

  </div>

)}

        </section>

      </main>

    </div>
  );
}

export default Dashboard;