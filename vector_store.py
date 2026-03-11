from typing import Dict, List, Tuple

import numpy as np


class InMemoryVectorStore:
    """
    Simple in-memory vector store per job_id.
    Stores embeddings and associated chunk texts.
    """

    def __init__(self) -> None:
        self._store: Dict[str, Dict[str, object]] = {}

    def add(self, job_id: str, embeddings: List[List[float]], texts: List[str]) -> None:
        if not embeddings or not texts:
            return
        emb_array = np.array(embeddings, dtype=np.float32)
        # L2-normalize for cosine similarity
        norms = np.linalg.norm(emb_array, axis=1, keepdims=True) + 1e-8
        emb_array = emb_array / norms
        self._store[job_id] = {
            "embeddings": emb_array,
            "texts": texts,
        }

    def query(self, job_id: str, query_embedding: List[float], top_k: int = 5) -> List[Tuple[str, float]]:
        entry = self._store.get(job_id)
        if not entry:
            return []
        emb_array: np.ndarray = entry["embeddings"]  # type: ignore[assignment]
        texts: List[str] = entry["texts"]  # type: ignore[assignment]

        q = np.array(query_embedding, dtype=np.float32)
        q_norm = np.linalg.norm(q) + 1e-8
        q = q / q_norm

        # Cosine similarity since vectors are normalized
        sims = emb_array @ q
        idxs = np.argsort(-sims)[:top_k]
        return [(texts[int(i)], float(sims[int(i)])) for i in idxs]


vector_store = InMemoryVectorStore()

