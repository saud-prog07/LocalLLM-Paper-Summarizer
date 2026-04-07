package com.research.summarizer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfService {

    // I learned that PDFBox can extract text page by page
    // so I'm using that to read the uploaded PDF

    public String extractText(MultipartFile file) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }

    public int getPageCount(MultipartFile file) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());
        int pages = document.getNumberOfPages();
        document.close();
        return pages;
    }

    // The problem I ran into: Claude API has a token limit
    // so I can't just send the whole PDF text at once for big papers.
    // Solution: split the text into smaller pieces (chunks) and summarize each one.
    // I picked ~2000 words per chunk after some trial and error.

    public List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        // split by words to keep chunk sizes manageable
        String[] words = text.split("\\s+");
        int chunkSize = 1500; // words per chunk

        StringBuilder currentChunk = new StringBuilder();
        int wordCount = 0;

        for (String word : words) {
            currentChunk.append(word).append(" ");
            wordCount++;

            if (wordCount >= chunkSize) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                wordCount = 0;
            }
        }

        // add the last remaining chunk if it has content
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
