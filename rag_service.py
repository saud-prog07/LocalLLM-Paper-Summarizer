from typing import List

from embeddings_service import embed_texts
from llm_service import summarize_chunk
from vector_store import vector_store


def index_chunks(job_id: str, chunks: List[str]) -> None:
    """
    Compute embeddings for chunks of a job and store them in the vector store.
    """
    embeddings = embed_texts(chunks)
    if embeddings:
        vector_store.add(job_id, embeddings, chunks)


def answer_question(job_id: str, question: str, top_k: int = 5) -> str:
    """
    Retrieve relevant chunks for a question and ask the LLM to answer using those chunks.
    """
    q_embs = embed_texts([question])
    if not q_embs:
        return "Could not generate embeddings for the question."

    matches = vector_store.query(job_id, q_embs[0], top_k=top_k)
    if not matches:
        return "No context available for this paper. Try re-uploading and summarizing first."

    context_blocks = []
    for text, score in matches:
        context_blocks.append(f"[relevance={score:.3f}]\n{text}")

    context = "\n\n".join(context_blocks)

    prompt = f"""
You are an expert assistant helping with questions about a specific research paper.

You are given the following relevant excerpts from the paper:

{context}

Question: {question}

Answer the question concisely, citing only information that can reasonably be inferred from the provided context.
If the context is insufficient, say so explicitly.
"""

    return summarize_chunk(prompt)

