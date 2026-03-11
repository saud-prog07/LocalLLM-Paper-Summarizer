import os
import requests
from typing import List

EMBED_URL = os.getenv("OLLAMA_EMBED_URL", "http://localhost:11434/api/embed")
EMBED_MODEL = os.getenv("OLLAMA_EMBED_MODEL", "embeddinggemma")


def embed_texts(texts: List[str]) -> list[list[float]]:
    """
    Generate embeddings for a list of texts using Ollama's /api/embed endpoint.
    Returns a list of embedding vectors (lists of floats), one per text.
    """
    if not texts:
        return []

    response = requests.post(
        EMBED_URL,
        json={
            "model": EMBED_MODEL,
            "input": texts,
        },
        timeout=600,
    )
    response.raise_for_status()
    data = response.json()
    return data.get("embeddings", [])

