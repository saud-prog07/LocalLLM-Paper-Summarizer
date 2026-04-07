package com.research.summarizer.controller;

import com.research.summarizer.service.ClaudeService;
import com.research.summarizer.service.PdfService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class SummarizerController {

    private final PdfService pdfService;
    private final ClaudeService claudeService;

    // using constructor injection - learned this is better than @Autowired on fields
    public SummarizerController(PdfService pdfService, ClaudeService claudeService) {
        this.pdfService = pdfService;
        this.claudeService = claudeService;
    }

    // show the home page
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // handle the PDF upload and summarize it
    @PostMapping("/summarize")
    public String summarize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", defaultValue = "Research Paper") String title,
            Model model) {

        // basic validation
        if (file.isEmpty()) {
            model.addAttribute("error", "Please upload a PDF file.");
            return "index";
        }

        if (!file.getOriginalFilename().endsWith(".pdf")) {
            model.addAttribute("error", "Only PDF files are supported.");
            return "index";
        }

        try {
            long startTime = System.currentTimeMillis();

            // step 1: extract text from the PDF
            String fullText = pdfService.extractText(file);
            int totalPages = pdfService.getPageCount(file);

            if (fullText.isBlank()) {
                model.addAttribute("error", "Could not extract text from this PDF. It might be a scanned image.");
                return "index";
            }

            // step 2: split into chunks so we don't exceed the API token limit
            List<String> chunks = pdfService.splitIntoChunks(fullText);

            // step 3: summarize each chunk
            List<String> chunkSummaries = chunks.stream()
                    .map(chunk -> claudeService.summarizeChunk(chunk))
                    .toList();

            // step 4: combine all chunk summaries into one final summary
            String finalSummary;
            if (chunkSummaries.size() == 1) {
                // no need to combine if the paper was short enough to fit in one chunk
                finalSummary = chunkSummaries.get(0);
            } else {
                finalSummary = claudeService.combineSummaries(chunkSummaries, title);
            }

            long timeTaken = (System.currentTimeMillis() - startTime) / 1000;

            // pass results to the HTML page
            model.addAttribute("summary", finalSummary);
            model.addAttribute("title", title);
            model.addAttribute("pages", totalPages);
            model.addAttribute("chunks", chunks.size());
            model.addAttribute("timeTaken", timeTaken);
            model.addAttribute("fileName", file.getOriginalFilename());

        } catch (Exception e) {
            model.addAttribute("error", "Something went wrong: " + e.getMessage());
            return "index";
        }

        return "result";
    }
}
