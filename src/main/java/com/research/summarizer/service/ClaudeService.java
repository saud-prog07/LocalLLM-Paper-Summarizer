package com.research.summarizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class ClaudeService {

    // I'm reading the API key from application.properties
    // so I don't hardcode it in the source code (learned this is bad practice)
    @Value("${claude.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ClaudeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Claude API base URL
        this.webClient = WebClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    // This method summarizes a single chunk of text
    public String summarizeChunk(String textChunk) {
        String prompt = "Please summarize the following section of a research paper. " +
                "Keep the key points, findings, and important details:\n\n" + textChunk;

        return callClaudeApi(prompt);
    }

    // After summarizing all chunks separately, I combine those summaries
    // into one final clean summary. This way even a 100 page paper works.
    public String combineSummaries(List<String> chunkSummaries, String paperTitle) {
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < chunkSummaries.size(); i++) {
            combined.append("Part ").append(i + 1).append(":\n");
            combined.append(chunkSummaries.get(i)).append("\n\n");
        }

        String prompt = "You are given summaries of different sections of a research paper titled: \"" + paperTitle + "\".\n\n" +
                "Please combine them into one well-structured final summary with these sections:\n" +
                "1. Overview\n" +
                "2. Key Findings\n" +
                "3. Methodology\n" +
                "4. Conclusion\n\n" +
                "Here are the section summaries:\n\n" + combined;

        return callClaudeApi(prompt);
    }

    // The actual API call - I looked at the Anthropic docs to figure out the request format
    private String callClaudeApi(String userMessage) {
        try {
            // build the request body as a JSON string
            // I tried using a Map first but building it manually was easier to understand
            String requestBody = "{"
                    + "\"model\": \"claude-sonnet-4-20250514\","
                    + "\"max_tokens\": 1024,"
                    + "\"messages\": ["
                    + "  {\"role\": \"user\", \"content\": " + objectMapper.writeValueAsString(userMessage) + "}"
                    + "]"
                    + "}";

            String response = webClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // using block() for now since I haven't learned reactive yet

            // parse the response to get just the text content
            JsonNode root = objectMapper.readTree(response);
            return root.get("content").get(0).get("text").asText();

        } catch (Exception e) {
            // TODO: handle errors better - for now just return an error message
            return "Error getting summary: " + e.getMessage();
        }
    }
}
