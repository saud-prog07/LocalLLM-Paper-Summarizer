from pathlib import Path

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, FileResponse

from summary_service import start_summary_job, get_status, UPLOAD_DIR

app = FastAPI(title="AI-Powered Research Paper Summarizer")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/", response_class=HTMLResponse)
async def index():
    return FileResponse("frontend/index.html")


@app.post("/api/upload")
async def upload_pdf(file: UploadFile = File(...)):
    if file.content_type != "application/pdf":
        raise HTTPException(status_code=400, detail="Only PDF files are supported")

    UPLOAD_DIR.mkdir(exist_ok=True)
    dest = UPLOAD_DIR / file.filename
    with dest.open("wb") as f:
        f.write(await file.read())

    job_id = start_summary_job(dest)
    return {"job_id": job_id}


@app.get("/api/status/{job_id}")
async def job_status(job_id: str):
    job = get_status(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    return {"id": job_id, "status": job["status"], "error": job["error"]}


@app.get("/api/summarize/{job_id}")
async def get_summary(job_id: str):
    job = get_status(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")

    if job["status"] != "COMPLETED":
        raise HTTPException(status_code=202, detail=f"Job not completed. Status: {job['status']}")

    return {"id": job_id, "summary": job["summary"]}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)

