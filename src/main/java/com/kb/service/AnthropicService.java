package com.kb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Service to interact with Anthropic's Claude API.
 * Handles two things:
 *   1. Generating text embeddings (via a simple approach using Claude)
 *   2. Answering questions using RAG context
 */
@Service
public class AnthropicService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(AnthropicService.class);

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    public AnthropicService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Ask Claude a question using retrieved context chunks (RAG pattern).
     * The context is the relevant entries found from the knowledge base.
     */
    public String askWithContext(String userQuestion, List<String> contextChunks) {
        String context = String.join("\n\n---\n\n", contextChunks);

        String systemPrompt = """
                You are a personal knowledge assistant. The user has a personal knowledge base
                of notes, book highlights, articles, and thoughts they've collected over time.
                
                Your job is to answer their questions based ONLY on the context provided below.
                If the answer isn't in the context, say so honestly and suggest they add more notes on the topic.
                Be conversational, insightful, and help them connect ideas across different entries.
                
                KNOWLEDGE BASE CONTEXT:
                """ + context;

        return callClaude(systemPrompt, userQuestion);
    }

    /**
     * Generate a short AI summary for a knowledge entry.
     */
    public String generateSummary(String content) {
        String systemPrompt = """
                You are a helpful assistant that creates concise summaries.
                Given some content (could be notes, article text, or book highlights),
                generate a 2-3 sentence summary capturing the key insight.
                Be direct and informative. No fluff.
                """;

        return callClaude(systemPrompt, "Summarize this:\n\n" + content);
    }

    /**
     * Generate weekly reflection across all entries from the past week.
     */
    public String generateWeeklyReflection(List<String> recentEntries) {
        String entriesText = String.join("\n\n---\n\n", recentEntries);

        String systemPrompt = """
                You are a thoughtful reflection assistant. Given a person's recent knowledge base entries
                (notes, articles, books they've been reading), generate a weekly reflection that:
                1. Identifies the main themes they've been exploring
                2. Points out interesting connections between different entries
                3. Suggests one question worth thinking about this week
                
                Keep it personal, warm, and insightful. 3-4 paragraphs max.
                """;

        return callClaude(systemPrompt, "Here are my entries from this week:\n\n" + entriesText);
    }

    /**
     * Core method to call Claude API.
     */
    private String callClaude(String systemPrompt, String userMessage) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 1024);
        requestBody.put("system", systemPrompt);

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", userMessage);

        try {
            log.info("Calling Anthropic API with request: {}", requestBody.toPrettyString());
            String response = webClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("Anthropic API error response: {}", errorBody);
                            return Mono.error(new RuntimeException(errorBody));
                        })
                    )
                    .bodyToMono(String.class)
                    .block();
            log.info("Anthropic raw response: {}", response);

            JsonNode responseJson = objectMapper.readTree(response);
            return responseJson
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Anthropic API: " + e.getMessage(), e);
        }
    }
}
