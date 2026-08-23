package com.healthcare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Integrates with OpenAI-compatible LLM endpoints.
 * All failures are caught and surfaced as a fallback message — the system
 * never throws from here so a single LLM hiccup cannot break a booking.
 */
@Slf4j
@Service
public class LlmService {

    @Value("${app.llm.api-key}")
    private String apiKey;

    @Value("${app.llm.api-url}")
    private String apiUrl;

    @Value("${app.llm.model}")
    private String model;

    @Value("${app.llm.timeout-seconds}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------------------------------------------------------------
    // Pre-visit summary
    // ---------------------------------------------------------------

    /**
     * Returns a structured pre-visit summary for the doctor.
     * Result fields: urgencyLevel, chiefComplaint, suggestedQuestions (newline-separated).
     * On any failure, returns a PreVisitResult with llmError set.
     */
    public PreVisitResult generatePreVisitSummary(String symptoms) {
        String prompt = buildPreVisitPrompt(symptoms);
        try {
            String raw = callLlm(prompt);
            return parsePreVisitResponse(raw);
        } catch (Exception e) {
            log.error("LLM pre-visit summary failed: {}", e.getMessage());
            return PreVisitResult.failed("LLM unavailable: " + e.getMessage());
        }
    }

    /**
     * Returns a patient-friendly post-visit summary.
     * On any failure, returns a PostVisitResult with llmError set.
     */
    public PostVisitResult generatePostVisitSummary(String clinicalNotes) {
        String prompt = buildPostVisitPrompt(clinicalNotes);
        try {
            String raw = callLlm(prompt);
            return PostVisitResult.success(raw);
        } catch (Exception e) {
            log.error("LLM post-visit summary failed: {}", e.getMessage());
            return PostVisitResult.failed("LLM unavailable: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------

    private String callLlm(String userPrompt) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        String requestBody = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("model", model);
            put("messages", java.util.List.of(
                    new java.util.LinkedHashMap<>() {{
                        put("role", "system");
                        put("content", "You are a clinical assistant. Be concise and structured.");
                    }},
                    new java.util.LinkedHashMap<>() {{
                        put("role", "user");
                        put("content", userPrompt);
                    }}
            ));
            put("temperature", 0.3);
            put("max_tokens", 600);
        }});

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "no body";
                throw new IOException("LLM API error " + response.code() + ": " + errBody);
            }
            String body = response.body().string();
            JsonNode root = objectMapper.readTree(body);
            return root.path("choices").get(0).path("message").path("content").asText();
        }
    }

    private String buildPreVisitPrompt(String symptoms) {
        return "Analyse these symptoms and return EXACTLY in this format:\n" +
               "URGENCY: <Low|Medium|High>\n" +
               "CHIEF_COMPLAINT: <one sentence>\n" +
               "QUESTION_1: <suggested question for doctor>\n" +
               "QUESTION_2: <suggested question for doctor>\n" +
               "QUESTION_3: <suggested question for doctor>\n\n" +
               "Symptoms: " + symptoms;
    }

    private String buildPostVisitPrompt(String clinicalNotes) {
        return "Convert these clinical notes into a patient-friendly summary with medication schedule " +
               "and follow-up steps. Use plain, easy-to-understand language. " +
               "Structure it with: Summary, Medications, Follow-up Steps.\n\n" +
               "Clinical Notes: " + clinicalNotes;
    }

    private PreVisitResult parsePreVisitResponse(String raw) {
        String urgency = "Low";
        String chiefComplaint = "";
        StringBuilder questions = new StringBuilder();

        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("URGENCY:")) {
                urgency = trimmed.substring("URGENCY:".length()).trim();
            } else if (trimmed.startsWith("CHIEF_COMPLAINT:")) {
                chiefComplaint = trimmed.substring("CHIEF_COMPLAINT:".length()).trim();
            } else if (trimmed.startsWith("QUESTION_")) {
                int colonIdx = trimmed.indexOf(':');
                if (colonIdx >= 0) {
                    String q = trimmed.substring(colonIdx + 1).trim();
                    if (!q.isEmpty()) {
                        if (!questions.isEmpty()) questions.append("\n");
                        questions.append(q);
                    }
                }
            }
        }

        // Normalise urgency to known values
        if (!urgency.equalsIgnoreCase("High") && !urgency.equalsIgnoreCase("Medium")) {
            urgency = "Low";
        }

        return PreVisitResult.builder()
                .urgencyLevel(urgency)
                .chiefComplaint(chiefComplaint.isEmpty() ? "Not specified" : chiefComplaint)
                .suggestedQuestions(questions.isEmpty() ? raw : questions.toString())
                .rawResponse(raw)
                .success(true)
                .build();
    }

    // ---------------------------------------------------------------
    // Result DTOs (inner records so they stay close to the service)
    // ---------------------------------------------------------------

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreVisitResult {
        private boolean success;
        private String urgencyLevel;
        private String chiefComplaint;
        private String suggestedQuestions;
        private String rawResponse;
        private String errorMessage;

        public static PreVisitResult failed(String error) {
            return PreVisitResult.builder()
                    .success(false)
                    .urgencyLevel("Low")
                    .chiefComplaint("Unable to generate summary")
                    .suggestedQuestions("")
                    .errorMessage(error)
                    .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PostVisitResult {
        private boolean success;
        private String patientSummary;
        private String rawResponse;
        private String errorMessage;

        public static PostVisitResult success(String raw) {
            return PostVisitResult.builder()
                    .success(true)
                    .patientSummary(raw)
                    .rawResponse(raw)
                    .build();
        }

        public static PostVisitResult failed(String error) {
            return PostVisitResult.builder()
                    .success(false)
                    .patientSummary("Summary generation is temporarily unavailable.")
                    .errorMessage(error)
                    .build();
        }
    }
}
