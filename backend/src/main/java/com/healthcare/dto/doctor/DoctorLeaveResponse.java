package com.healthcare.dto.doctor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DoctorLeaveResponse {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private LocalDate leaveDate;
    private String reason;
    private Boolean patientsNotified;
    private LocalDateTime createdAt;
}
