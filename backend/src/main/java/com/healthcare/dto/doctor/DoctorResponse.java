package com.healthcare.dto.doctor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class DoctorResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phoneNumber;
    private String specialisation;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private Integer slotDurationMinutes;
    private Integer maxPatientsPerDay;
    private String biography;
    private Double consultationFee;
}
