package com.healthcare.dto.symptom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SymptomFormResponse {

    private Long id;
    private Long appointmentId;
    private String symptoms;
    private String symptomDuration;
    private String severitySelfReported;
    private String currentMedications;

    // LLM generated fields
    private String llmUrgencyLevel;
    private String llmChiefComplaint;
    private String llmSuggestedQuestions;
    private Boolean llmProcessed;
    private String llmError;

    private LocalDateTime createdAt;
}
