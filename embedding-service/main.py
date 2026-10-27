from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
EXPECTED_DIMENSION = 384

app = FastAPI(title="DocMind Embedding Service")

model: SentenceTransformer | None = None


class EmbedRequest(BaseModel):
    text: str = Field(min_length=1)


def get_model() -> SentenceTransformer:
    global model
    if model is None:
        print("Loading embedding model...")
        model = SentenceTransformer(MODEL_NAME)
        print("Embedding model loaded successfully!")
    return model


@app.on_event("startup")
def startup() -> None:
    get_model()


@app.get("/")
def root():
    return {
        "service": "DocMind Embedding Service",
        "status": "running",
        "model": MODEL_NAME,
        "dimensions": EXPECTED_DIMENSION,
    }


@app.post("/embed")
def create_embedding(request: EmbedRequest):
    text = request.text.strip()
    if not text:
        raise HTTPException(status_code=400, detail="Text must not be blank")

    try:
        embedding = get_model().encode(text)
        values = embedding.tolist()
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Embedding generation failed") from exc

    if len(values) != EXPECTED_DIMENSION:
        raise HTTPException(
            status_code=500,
            detail=f"Unexpected embedding dimension: {len(values)}",
        )

    if any(value is None or value != value or value in (float("inf"), float("-inf")) for value in values):
        raise HTTPException(status_code=500, detail="Embedding contains a non-finite value")

    return {
        "embedding": values,
        "dimensions": len(values),
    }
