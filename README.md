# Research Paper Summarizer

A Spring Boot web app that lets you upload a research paper (PDF) and get an AI-generated summary using the Claude API.

I built this project to get hands-on experience with Spring Boot and working with external APIs. It was also a good challenge because research papers can be really long, so I had to figure out how to handle the API's token limits.

---

## What it does

- Upload any research paper as a PDF
- The app extracts the text using Apache PDFBox
- Splits the text into smaller chunks (because the Claude API has token limits)
- Sends each chunk to Claude API for summarization
- Combines all the chunk summaries into one final structured summary
- Shows the result on a clean results page

---

## Tech stack

- **Java 17**
- **Spring Boot 3.2** (web, thymeleaf)
- **Apache PDFBox** - for reading PDF files
- **Claude API (Anthropic)** - for AI summarization
- **Thymeleaf** - for the HTML templates
- **Maven** - for dependency management

---

## How to run it

### 1. Get a Claude API key
Sign up at https://console.anthropic.com and create an API key.

### 2. Set your API key
Either set it as an environment variable:
```
export CLAUDE_API_KEY=sk-ant-your-key-here
```

Or edit `src/main/resources/application.properties` directly (don't commit this to GitHub):
```
claude.api.key=sk-ant-your-key-here
```

### 3. Run the app
```bash
mvn spring-boot:run
```

### 4. Open the browser
Go to http://localhost:8080

---

## Project structure

```
src/
├── main/
│   ├── java/com/research/summarizer/
│   │   ├── PaperSummarizerApplication.java   # main class
│   │   ├── controller/
│   │   │   └── SummarizerController.java     # handles web requests
│   │   └── service/
│   │       ├── PdfService.java               # PDF reading + chunking
│   │       └── ClaudeService.java            # Claude API calls
│   └── resources/
│       ├── application.properties
│       └── templates/
│           ├── index.html                    # upload page
│           └── result.html                   # summary results page
└── test/
    └── PdfServiceTest.java
```

---

## Challenges I faced

**Token limits:** The biggest issue was that long papers couldn't be sent to the API in one go. I solved this by splitting the text into chunks of ~1500 words, summarizing each chunk separately, then combining them into a final summary.

**PDF text extraction:** Some PDFs are scanned images and PDFBox can't extract text from those. I added a check for that and show an error message.

---

## What I'd improve next

- Add async processing so the page doesn't hang while waiting for the API
- Support uploading multiple papers
- Let users download the summary as a text file
- Maybe add a loading spinner with progress updates

---

## Running the tests

```bash
mvn test
```
