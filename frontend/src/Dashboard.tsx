import "./Dashboard.css";

function Dashboard() {

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

          <button className="upload-button">
            + Upload Document
          </button>

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

        </section>

      </main>

    </div>
  );
}

export default Dashboard;