package com.research.summarizer;

import com.research.summarizer.service.PdfService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// I wrote these tests to check that my chunking logic works correctly
class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    void emptyTextShouldReturnNoChunks() {
        List<String> chunks = pdfService.splitIntoChunks("");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void shortTextShouldFitInOneChunk() {
        String shortText = "This is a short abstract. It describes the research paper briefly.";
        List<String> chunks = pdfService.splitIntoChunks(shortText);
        assertEquals(1, chunks.size());
    }

    @Test
    void longTextShouldSplitIntoMultipleChunks() {
        // generate a long string (more than 1500 words)
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            longText.append("word").append(i).append(" ");
        }

        List<String> chunks = pdfService.splitIntoChunks(longText.toString());
        assertTrue(chunks.size() > 1, "Long text should produce more than one chunk");
    }

    @Test
    void chunksShouldNotBeEmpty() {
        String text = "The quick brown fox. ".repeat(100);
        List<String> chunks = pdfService.splitIntoChunks(text);
        for (String chunk : chunks) {
            assertFalse(chunk.isBlank(), "No chunk should be empty");
        }
    }
}
