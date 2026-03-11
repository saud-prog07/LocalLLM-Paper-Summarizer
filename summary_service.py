import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Dict

from chunk_service import create_chunks
from llm_service import summarize_chunk, final_summary
from pdf_service import extract_text

UPLOAD_DIR = Path("uploads")
UPLOAD_DIR.mkdir(exist_ok=True)

executor = ThreadPoolExecutor(max_workers=5)


class JobStatus:
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


jobs: Dict[str, dict] = {}


def start_summary_job(file_path: Path) -> str:
    job_id = str(uuid.uuid4())
    jobs[job_id] = {
        "id": job_id,
        "status": JobStatus.PENDING,
        "summary": None,
        "error": None,
    }

    def run():
        jobs[job_id]["status"] = JobStatus.PROCESSING
        try:
            text = extract_text(str(file_path))
            if not text.strip():
                raise ValueError("No text extracted from PDF")

            chunks = create_chunks(text, chunk_size_tokens=1200)
            if not chunks:
                raise ValueError("No chunks generated from text")

            # Parallel chunk summarization
            chunk_summaries = list(executor.map(summarize_chunk, chunks))

            # Final aggregation pass
            final = final_summary(chunk_summaries)

            jobs[job_id]["summary"] = final
            jobs[job_id]["status"] = JobStatus.COMPLETED
        except Exception as e:
            jobs[job_id]["status"] = JobStatus.FAILED
            jobs[job_id]["error"] = str(e)

    executor.submit(run)
    return job_id


def get_status(job_id: str) -> dict | None:
    return jobs.get(job_id)

