# DocMind

Intelligent document knowledge base: upload documents, generate embeddings, and search your own files with natural-language queries.

This project does **not** generate AI answers, summaries, citations, or chat. It retrieves the most similar passages from documents you uploaded.

---

## Architecture

| Layer | Technology |
|---|---|
| Frontend | React, TypeScript, Vite |
| Backend | Java 21, Spring Boot 3.5.6, Spring Security + JWT |
| Database | PostgreSQL 16 + pgvector (Docker) |
| Embedding service | Python FastAPI + `sentence-transformers/all-MiniLM-L6-v2` (384 dimensions) |

---

## Prerequisites

- **Java 21** — set `JAVA_HOME`
- **Node.js 18+** and npm
- **Docker Desktop** (PostgreSQL + pgvector)
- **Python 3.10+** (3.12 was used previously)

---

## Setup and running (Windows PowerShell)

Use **four terminals** from the project root `D:\Projects\DocMind`.

### 1. PostgreSQL

```powershell
docker compose up -d
```

- Port: `5432`
- Database / user: `docmind`
- Local development password is set in `docker-compose.yml` for convenience. Do not reuse it in production.

### 2. Embedding service

```powershell
cd embedding-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --host 127.0.0.1 --port 8000
```

Health check:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8000/" -Method GET
```

The first start downloads the MiniLM model and can take several minutes.

### 3. Backend

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run
```

Backend: **http://localhost:8080**

### 4. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend: **http://localhost:5173** (Vite may choose 5174+ if 5173 is busy; CORS allows 5173–5180).

---

## Environment variables

See `.env.example` for placeholders. Never commit a real `.env`.

| Variable | Default (development) | Notes |
|---|---|---|
| `JWT_SECRET` | documented local fallback | **Unsafe for production.** Start with `--spring.profiles.active=prod` so a missing/weak secret fails startup. |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/docmind` | |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `docmind` / `docmind` | Local Docker only |
| `EMBEDDING_BASE_URL` | `http://127.0.0.1:8000` | |
| `UPLOAD_DIR` | `uploads` | Relative to the backend working directory |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Frontend build-time API origin |

---

## Implemented features

- Register / login with BCrypt password hashing and JWT
- Current-user endpoint `GET /api/me`
- Authenticated PDF (and other Tika-supported) upload, list, and delete
- Text extraction, 1000-character chunking, 384-d embeddings
- Semantic search with pgvector cosine distance, limited to the authenticated user's documents
- React dashboard for that workflow

---

## API

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Register |
| POST | `/api/auth/login` | None | Login, returns JWT |
| GET | `/api/me` | JWT | Current user email |

Register body:

```json
{
  "fullName": "Alice Smith",
  "email": "alice@example.com",
  "password": "secret123"
}
```

### Documents (JWT required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/documents/upload` | Multipart field `file` |
| GET | `/api/documents` | List the caller's documents |
| DELETE | `/api/documents/{id}` | Delete document, chunks, and stored file |

### Search (JWT required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/search` | Semantic search (primary) |
| POST | `/api/documents/search` | Same handler (compatibility) |

Request:

```json
{
  "query": "What is a security threat?",
  "limit": 10
}
```

`limit` is optional (default 10, maximum 20).

Response items:

- `documentId`, `documentName`, `chunkIndex`, `content`
- `distance` — pgvector **cosine distance**. Lower is more similar. This is not a 0–100 percentage.

---

## Tests

Backend (from `backend/`):

```powershell
.\mvnw.cmd clean verify
```

Repository tests that need PostgreSQL + pgvector use Testcontainers and are skipped if Docker is not running.

Frontend:

```powershell
cd frontend
npm run build
npm run lint
```

Python (uses a fake model; does not download MiniLM):

```powershell
cd embedding-service
.\.venv\Scripts\Activate.ps1
python -m pytest -q
```

---

## Troubleshooting

- **POST `/api/search` used to return HTTP 403** while `GET /api/documents` worked. Causes that were fixed: the `/api/search` mapping was missing from the committed tree, Spring Security 6 filtered `ERROR` dispatches so search exceptions became `403 Forbidden`, and unauthenticated calls used the default 403 entry point. Authenticated search should now return 200 or a real error (400/503). Missing/invalid JWT returns **401**.
- **503 on upload or search:** start the embedding service on port 8000.
- **Database errors:** `docker compose up -d` and confirm port 5432 is the pgvector container.
- **CORS errors:** use a Vite origin in `http://localhost:5173`–`5180`.

---

## Known limitations

- No chat, summarization, or generated answers.
- JWTs last 24 hours; there is no refresh token.
- Upload embedding is synchronous; large PDFs wait until every chunk is embedded.
- `hibernate.ddl-auto=update` is used for local development, not a production migration tool.
- Local Docker database credentials are for development only.

---

## Project structure

```
DocMind/
├── backend/           Spring Boot API
├── embedding-service/ FastAPI embeddings
├── frontend/          React + Vite
└── docker-compose.yml PostgreSQL + pgvector
```
