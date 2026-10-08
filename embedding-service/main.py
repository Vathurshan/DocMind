from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI(title="DocMind Embedding Service")

print("Loading embedding model...")
model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
print("Embedding model loaded successfully!")


class EmbedRequest(BaseModel):
    text: str


@app.get("/")
def root():
    return {
        "service": "DocMind Embedding Service",
        "status": "running"
    }


@app.post("/embed")
def create_embedding(request: EmbedRequest):
    embedding = model.encode(request.text)

    return {
        "embedding": embedding.tolist(),
        "dimensions": len(embedding)
    }