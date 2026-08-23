package com.healthcare.dto.patient;

import com.healthcare.entity.Patient;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientRequest {

    private LocalDate dateOfBirth;
    private Patient.Gender gender;
    private String bloodGroup;
    private String allergies;
    private String emergencyContactName;
    private String emergencyContactPhone;
}
