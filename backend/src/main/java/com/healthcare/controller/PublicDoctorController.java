package com.healthcare.controller;

import com.healthcare.dto.appointment.AvailableSlotsResponse;
import com.healthcare.dto.common.ApiResponse;
import com.healthcare.dto.doctor.DoctorResponse;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Unauthenticated endpoints for browsing doctors and checking slot availability.
 * Matched by the SecurityConfig permit: GET /api/doctors/public/**
 */
@RestController
@RequestMapping("/api/doctors/public")
@RequiredArgsConstructor
public class PublicDoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> listDoctors(
            @RequestParam(required = false) String specialisation) {
        List<DoctorResponse> docs = specialisation != null
                ? doctorService.searchBySpecialisation(specialisation)
                : doctorService.getAllDoctors();
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.searchBySpecialisation("").stream()
                .filter(d -> d.getId().equals(doctorId)).findFirst()
                .orElseGet(() -> doctorService.getAllDoctors().stream()
                        .filter(d -> d.getId().equals(doctorId)).findFirst().orElseThrow())));
    }

    @GetMapping("/{doctorId}/slots")
    public ResponseEntity<ApiResponse<AvailableSlotsResponse>> getSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAvailableSlots(doctorId, date)));
    }
}
