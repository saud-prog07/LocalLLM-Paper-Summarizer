import os
import requests

OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/api/generate")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "phi3")


def summarize_chunk(chunk: str) -> str:
    prompt = f"""
You are an expert assistant summarizing academic research papers.

Summarize the following section clearly and concisely for a technical reader.
Focus on:
- Main idea
- Key findings
- Important methods or results

Text:
{chunk}
"""

    response = requests.post(
        OLLAMA_URL,
        json={
            "model": OLLAMA_MODEL,
            "prompt": prompt,
            "stream": False,
        },
        timeout=600,
    )
    response.raise_for_status()
    data = response.json()
    return (data.get("response") or "").strip()


def final_summary(chunk_summaries: list[str]) -> str:
    combined = "\n\n".join(chunk_summaries)

    prompt = f"""
You are an expert at synthesizing research paper summaries.

You are given partial summaries of different sections of a single academic paper.
Write ONE coherent final summary with this structure:

1. Key contributions
2. Methodology
3. Results and findings
4. Limitations and future work (if present)

Keep it concise (400–800 words) and avoid repetition.

Partial summaries:
{combined}
"""

    return summarize_chunk(prompt)

