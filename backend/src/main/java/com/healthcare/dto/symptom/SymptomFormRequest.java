package com.healthcare.dto.symptom;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SymptomFormRequest {

    @NotBlank(message = "Symptoms description is required")
    private String symptoms;

    private String symptomDuration;

    private String severitySelfReported;

    private String currentMedications;
}
