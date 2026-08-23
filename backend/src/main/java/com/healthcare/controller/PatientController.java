package com.healthcare.controller;

import com.healthcare.dto.appointment.*;
import com.healthcare.dto.common.ApiResponse;
import com.healthcare.dto.doctor.DoctorResponse;
import com.healthcare.dto.patient.PatientRequest;
import com.healthcare.dto.patient.PatientResponse;
import com.healthcare.dto.postvisit.PostVisitResponse;
import com.healthcare.dto.symptom.SymptomFormRequest;
import com.healthcare.dto.symptom.SymptomFormResponse;
import com.healthcare.entity.SlotHold;
import com.healthcare.repository.UserRepository;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.DoctorService;
import com.healthcare.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final UserRepository userRepository;

    // --- Profile ---

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PatientResponse>> getProfile(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(patientService.getMyProfile(resolveUserId(ud))));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<PatientResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody PatientRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                patientService.updateMyProfile(resolveUserId(ud), req)));
    }

    // --- Doctor discovery ---

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getDoctors(
            @RequestParam(required = false) String specialisation) {
        List<DoctorResponse> docs = specialisation != null
                ? doctorService.searchBySpecialisation(specialisation)
                : doctorService.getAllDoctors();
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @GetMapping("/doctors/{doctorId}/slots")
    public ResponseEntity<ApiResponse<AvailableSlotsResponse>> getSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAvailableSlots(doctorId, date)));
    }

    // --- Booking flow ---

    @PostMapping("/appointments/hold")
    public ResponseEntity<ApiResponse<Long>> holdSlot(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody SlotHoldRequest req) {
        SlotHold hold = appointmentService.holdSlot(resolveUserId(ud), req);
        return ResponseEntity.ok(ApiResponse.success("Slot held for " + hold.getExpiresAt(), hold.getId()));
    }

    @PostMapping("/appointments/{holdId}/confirm")
    public ResponseEntity<ApiResponse<AppointmentResponse>> confirmBooking(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long holdId) {
        return ResponseEntity.ok(ApiResponse.success("Appointment booked",
                appointmentService.toResponse(appointmentService.confirmBooking(resolveUserId(ud), holdId))));
    }

    // --- Symptom form ---

    @PostMapping("/appointments/{appointmentId}/symptoms")
    public ResponseEntity<ApiResponse<SymptomFormResponse>> submitSymptoms(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long appointmentId,
            @Valid @RequestBody SymptomFormRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Symptom form submitted",
                patientService.submitSymptomForm(resolveUserId(ud), appointmentId, req)));
    }

    @GetMapping("/appointments/{appointmentId}/symptoms")
    public ResponseEntity<ApiResponse<SymptomFormResponse>> getSymptoms(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                patientService.getSymptomForm(resolveUserId(ud), appointmentId)));
    }

    // --- Appointments ---

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyAppointments(
            @AuthenticationPrincipal UserDetails ud) {
        List<AppointmentResponse> list = appointmentService.getAppointmentsForPatient(resolveUserId(ud))
                .stream().map(appointmentService::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @DeleteMapping("/appointments/{appointmentId}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long appointmentId,
            @RequestBody(required = false) CancelRequest req) {
        String reason = req != null ? req.getReason() : null;
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled",
                appointmentService.toResponse(appointmentService.cancel(appointmentId, resolveUserId(ud), reason))));
    }

    @PutMapping("/appointments/{appointmentId}/reschedule")
    public ResponseEntity<ApiResponse<AppointmentResponse>> reschedule(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long appointmentId,
            @Valid @RequestBody RescheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Appointment rescheduled",
                appointmentService.toResponse(appointmentService.reschedule(appointmentId, resolveUserId(ud), req))));
    }

    // --- Post-visit summary ---

    @GetMapping("/appointments/{appointmentId}/post-visit")
    public ResponseEntity<ApiResponse<PostVisitResponse>> getPostVisit(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getPostVisit(appointmentId)));
    }

    // --- Helper ---

    private Long resolveUserId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow().getId();
    }
}
