package com.healthcare.dto.patient;

import com.healthcare.entity.Patient;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PatientResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Patient.Gender gender;
    private String bloodGroup;
    private String allergies;
    private String emergencyContactName;
    private String emergencyContactPhone;
}
