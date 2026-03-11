# AI-Powered Research Paper Summarizer

Fast, local-first summarization for large academic PDFs using **FastAPI**, **Ollama (Phi‑3 or Llama‑3)**, and **pdfplumber**.  
Upload a paper, the backend chunks and summarizes it with a local LLM, then aggregates into a clean final summary.

---

## 1. Features

- **PDF upload API** (FastAPI)
  - `POST /api/upload` – upload a paper, start a background job
  - `GET /api/status/{job_id}` – check processing status
  - `GET /api/summarize/{job_id}` – fetch the final summary
- **Robust PDF text extraction** with `pdfplumber`
- **Chunking layer** (~1200‑token chunks) to handle 10–100+ page PDFs
- **Parallel summarization** of chunks with `ThreadPoolExecutor`
- **Final aggregation pass** to produce one coherent summary
- **Local LLM via Ollama** (no external API keys, no rate limits)
- **Simple HTML + JS frontend** for upload, progress, and result display

---

## 2. Tech Stack

- **Backend**: FastAPI (Python 3.13)
- **LLM**: Ollama (default model: `phi3`, configurable)
- **PDF processing**: `pdfplumber`
- **Parallelism**: `concurrent.futures.ThreadPoolExecutor`
- **Frontend**: Static HTML + vanilla JS

Project layout:

```text
project/
│
├── app.py                # FastAPI server
├── pdf_service.py        # PDF → full text
├── chunk_service.py      # Text chunking
├── llm_service.py        # Calls Ollama
├── summary_service.py    # Job management + parallel summarization
├── utils/
│   └── tokenizer.py      # Token/word-based chunking helper
│
├── uploads/              # (created at runtime) uploaded PDFs
├── frontend/
│   └── index.html        # Upload UI
│
├── requirements.txt
└── README.md
```

---

## 3. Setup

### 3.1. Prerequisites

- **Python** 3.10+ (you have 3.13)
- **Ollama** installed and available in `PATH`
- At least one local model pulled, e.g.:

```bash
ollama pull phi3
# or
ollama pull llama3
```

### 3.2. Install Python dependencies

From the project root:

```bash
pip install -r requirements.txt
```

### 3.3. Start Ollama

In a separate terminal:

```bash
ollama serve
```

You can test the model manually:

```bash
ollama run phi3
```

---

## 4. Running the app

From the project root:

```powershell
cd "C:\Users\hp\OneDrive\Desktop\project"

# Choose the Ollama model (phi3 or llama3, etc.)
$env:OLLAMA_MODEL="phi3"

# Start FastAPI with Uvicorn
uvicorn app:app --reload --port 8002
```

Then open your browser at:

- `http://localhost:8002`

Upload a PDF and wait for the summary.

---

## 5. API Endpoints

### 5.1. `POST /api/upload`

Upload a PDF and start a summarization job.

- **Request**: `multipart/form-data` with field `file` (PDF)
- **Response**:

```json
{
  "job_id": "uuid-string"
}
```

### 5.2. `GET /api/status/{job_id}`

Check the status of a summarization job.

- **Response**:

```json
{
  "id": "uuid-string",
  "status": "PENDING | PROCESSING | COMPLETED | FAILED",
  "error": "error message or null"
}
```

### 5.3. `GET /api/summarize/{job_id}`

Fetch the final summary for a completed job.

- **On success (`COMPLETED`)**:

```json
{
  "id": "uuid-string",
  "summary": "Final summary text..."
}
```

- **If still running**: returns HTTP `202` with a message like `"Job not completed. Status: PROCESSING"`.

---

## 6. Module Breakdown

### 6.1. `app.py` – FastAPI server

- Registers CORS middleware (open to all origins for simplicity).
- Serves the frontend (`GET /` → `frontend/index.html`).
- Exposes:
  - `POST /api/upload` – saves PDF into `uploads/` and calls `start_summary_job()`.
  - `GET /api/status/{job_id}` – reads in-memory job state from `summary_service`.
  - `GET /api/summarize/{job_id}` – returns summary for completed jobs.

### 6.2. `pdf_service.py` – PDF → text

- Uses `pdfplumber` to iterate over pages and extract text.
- Concatenates page texts and returns a full document string.

### 6.3. `utils/tokenizer.py` and `chunk_service.py` – chunking

- `tokenizer.chunk_text(text, chunk_size=1200)`:
  - Naive “token” ≈ word split.
  - Splits into chunks of ~1200 words.
- `chunk_service.create_chunks(text, chunk_size_tokens=1200)`:
  - Thin wrapper around `chunk_text` for clarity and centralization.

### 6.4. `llm_service.py` – Ollama calls

- Reads:
  - `OLLAMA_URL` (default `http://localhost:11434/api/generate`).
  - `OLLAMA_MODEL` (default `phi3`).
- `summarize_chunk(chunk: str) -> str`:
  - Builds a focused summarization prompt for a single section.
  - Calls the Ollama HTTP API, returns `response["response"]` text.
- `final_summary(chunk_summaries: list[str]) -> str`:
  - Combines chunk summaries into one string.
  - Prompts the model to synthesize a coherent final summary (400–800 words).

### 6.5. `summary_service.py` – jobs & parallel processing

- Maintains:

```python
jobs: Dict[str, dict] = {}
```

per job ID, with:

- `id`, `status`, `summary`, `error`.

- Uses a global `ThreadPoolExecutor(max_workers=5)`:
  - Parallelizes `summarize_chunk` over all chunks.

- Flow in `start_summary_job(file_path: Path)`:
  1. Create a UUID job ID and initialize job state as `PENDING`.
  2. Submit a background task:
     - Extract text with `extract_text(path)`.
     - Chunk text via `create_chunks(text, 1200)`.
     - Map `summarize_chunk` over chunks in parallel.
     - Run `final_summary()` over partial summaries.
     - Store summary, mark job as `COMPLETED` or `FAILED`.

- `get_status(job_id: str)` returns the job dict or `None`.

### 6.6. `frontend/index.html` – upload UI

- Clean, single-page UI:
  - File input for PDFs.
  - Upload button.
  - Status text.
  - `<pre>` block for the final summary.
- JS flow:
  1. User selects a file (PDF), button enabled.
  2. On click, POST to `/api/upload` with `FormData`.
  3. Store `job_id` from response.
  4. Poll `/api/status/{job_id}` (with backoff) until `COMPLETED` or `FAILED`.
  5. On `COMPLETED`, fetch `/api/summarize/{job_id}` and show the summary.

---

## 7. Configuration

Environment variables (optional):

- `OLLAMA_URL` – default `http://localhost:11434/api/generate`
- `OLLAMA_MODEL` – default `phi3` (use any local Ollama model name)

Example (PowerShell):

```powershell
$env:OLLAMA_MODEL="phi3"
uvicorn app:app --reload --port 8002
```

---

## 8. Performance Tips

- Use a **smaller, faster model** (e.g. a 3B‑parameter variant) if available.
- Tweak:
  - Chunk size (e.g. 800–1200 words).
  - `max_workers` in `summary_service.py` based on your CPU/GPU.
- For extremely long PDFs, consider:
  - Limiting to first N pages for a “quick summary” mode.
  - Adding a RAG/embeddings layer (vector search) for question answering instead of summarizing everything.

---

## 9. Next Steps / Extensions

Potential improvements you can add on top of this base:

- **RAG (Retrieval Augmented Generation)**:
  - Store chunk embeddings in a vector store (FAISS/Chroma).
  - Add `/api/query` to answer Q&A over the paper using retrieved chunks.
- **Multi-model support**:
  - Let the frontend choose between models (`phi3`, `llama3`, etc.).
- **Persistence**:
  - Save summaries and metadata to a database instead of in-memory jobs.

This README describes the current working architecture using **Ollama locally** as requested. Adjust and extend as your use case grows. 

