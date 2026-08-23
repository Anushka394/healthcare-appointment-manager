package com.healthcare.service;

import com.healthcare.dto.doctor.DoctorLeaveRequest;
import com.healthcare.dto.doctor.DoctorLeaveResponse;
import com.healthcare.dto.doctor.DoctorRequest;
import com.healthcare.dto.doctor.DoctorResponse;
import com.healthcare.dto.postvisit.PostVisitRequest;
import com.healthcare.dto.postvisit.PostVisitResponse;
import com.healthcare.entity.*;
import com.healthcare.exception.BadRequestException;
import com.healthcare.exception.DuplicateResourceException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.exception.UnauthorizedException;
import com.healthcare.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorLeaveRepository doctorLeaveRepository;
    private final AppointmentRepository appointmentRepository;
    private final PostVisitRecordRepository postVisitRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LlmService llmService;
    private final EmailService emailService;
    private final AdminService adminService; // reuse toResponse mapper

    // ---------------------------------------------------------------
    // Profile
    // ---------------------------------------------------------------

    public DoctorResponse getMyProfile(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        return adminService.toResponse(doctor);
    }

    @Transactional
    public DoctorResponse updateMyProfile(Long userId, DoctorRequest req) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        if (req.getSpecialisation() != null) doctor.setSpecialisation(req.getSpecialisation());
        if (req.getWorkStartTime() != null) doctor.setWorkStartTime(req.getWorkStartTime());
        if (req.getWorkEndTime() != null) doctor.setWorkEndTime(req.getWorkEndTime());
        if (req.getSlotDurationMinutes() != null) doctor.setSlotDurationMinutes(req.getSlotDurationMinutes());
        if (req.getMaxPatientsPerDay() != null) doctor.setMaxPatientsPerDay(req.getMaxPatientsPerDay());
        if (req.getBiography() != null) doctor.setBiography(req.getBiography());
        if (req.getConsultationFee() != null) doctor.setConsultationFee(req.getConsultationFee());
        return adminService.toResponse(doctorRepository.save(doctor));
    }

    // ---------------------------------------------------------------
    // Schedule / appointments
    // ---------------------------------------------------------------

    public List<Appointment> getMyAppointments(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDescSlotStartTimeDesc(doctor.getId());
    }

    public List<Appointment> getAppointmentsForDate(Long userId, LocalDate date) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        return appointmentRepository.findByDoctorIdAndAppointmentDateOrderBySlotStartTime(doctor.getId(), date);
    }

    // ---------------------------------------------------------------
    // Leave (doctors can also request their own leave)
    // ---------------------------------------------------------------

    @Transactional
    public DoctorLeaveResponse requestLeave(Long userId, DoctorLeaveRequest req) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        return adminService.addLeave(doctor.getId(), req);
    }

    public List<DoctorLeaveResponse> getMyLeaves(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        return adminService.getLeavesForDoctor(doctor.getId());
    }

    // ---------------------------------------------------------------
    // Post-visit notes + LLM summary
    // ---------------------------------------------------------------

    @Transactional
    public PostVisitResponse submitPostVisit(Long userId, Long appointmentId, PostVisitRequest req) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        if (!appt.getDoctor().getId().equals(doctor.getId())) {
            throw new UnauthorizedException("This appointment does not belong to you");
        }
        if (appt.getStatus() != Appointment.AppointmentStatus.CONFIRMED
                && appt.getStatus() != Appointment.AppointmentStatus.PENDING_SYMPTOMS) {
            throw new BadRequestException("Post-visit notes can only be submitted for CONFIRMED appointments");
        }
        if (postVisitRecordRepository.findByAppointmentId(appointmentId).isPresent()) {
            throw new DuplicateResourceException("Post-visit record already exists for this appointment");
        }

        // Build LLM summary (graceful fallback on failure)
        LlmService.PostVisitResult llmResult = llmService.generatePostVisitSummary(req.getClinicalNotes());

        PostVisitRecord record = PostVisitRecord.builder()
                .appointment(appt)
                .clinicalNotes(req.getClinicalNotes())
                .diagnosis(req.getDiagnosis())
                .followUpDate(req.getFollowUpDate())
                .llmPatientSummary(llmResult.getPatientSummary())
                .llmRawResponse(llmResult.getRawResponse())
                .llmProcessed(llmResult.isSuccess())
                .llmError(llmResult.getErrorMessage())
                .build();

        record = postVisitRecordRepository.save(record);

        // Save prescriptions
        List<PostVisitResponse.PrescriptionDto> prescriptionDtos = List.of();
        if (req.getPrescriptions() != null && !req.getPrescriptions().isEmpty()) {
            List<Prescription> saved = req.getPrescriptions().stream().map(p -> Prescription.builder()
                    .appointment(appt)
                    .patient(appt.getPatient())
                    .medicationName(p.getMedicationName())
                    .dosage(p.getDosage())
                    .frequency(p.getFrequency())
                    .remindersPerDay(p.getRemindersPerDay() != null ? p.getRemindersPerDay() : 1)
                    .startDate(p.getStartDate() != null ? p.getStartDate() : LocalDate.now())
                    .endDate(p.getEndDate())
                    .instructions(p.getInstructions())
                    .reminderActive(true)
                    .build()).map(prescriptionRepository::save).collect(Collectors.toList());

            prescriptionDtos = saved.stream().map(p -> PostVisitResponse.PrescriptionDto.builder()
                    .id(p.getId()).medicationName(p.getMedicationName()).dosage(p.getDosage())
                    .frequency(p.getFrequency()).remindersPerDay(p.getRemindersPerDay())
                    .startDate(p.getStartDate()).endDate(p.getEndDate()).instructions(p.getInstructions())
                    .build()).collect(Collectors.toList());
        }

        // Mark appointment completed
        appt.setStatus(Appointment.AppointmentStatus.COMPLETED);
        appointmentRepository.save(appt);

        // Email patient-friendly summary
        try {
            emailService.sendPostVisitSummary(appt, llmResult.getPatientSummary());
        } catch (Exception e) {
            log.error("Post-visit summary email failed: {}", e.getMessage());
        }

        PostVisitRecord finalRecord = record;
        List<PostVisitResponse.PrescriptionDto> finalPrescriptionDtos = prescriptionDtos;
        return PostVisitResponse.builder()
                .id(finalRecord.getId())
                .appointmentId(appointmentId)
                .clinicalNotes(finalRecord.getClinicalNotes())
                .diagnosis(finalRecord.getDiagnosis())
                .followUpDate(finalRecord.getFollowUpDate())
                .llmPatientSummary(finalRecord.getLlmPatientSummary())
                .llmProcessed(finalRecord.getLlmProcessed())
                .llmError(finalRecord.getLlmError())
                .prescriptions(finalPrescriptionDtos)
                .createdAt(finalRecord.getCreatedAt())
                .build();
    }

    public PostVisitResponse getPostVisit(Long appointmentId) {
        PostVisitRecord r = postVisitRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Post-visit record not found for appointment " + appointmentId));
        List<PostVisitResponse.PrescriptionDto> prescriptionDtos = prescriptionRepository
                .findByAppointmentId(appointmentId).stream()
                .map(p -> PostVisitResponse.PrescriptionDto.builder()
                        .id(p.getId()).medicationName(p.getMedicationName()).dosage(p.getDosage())
                        .frequency(p.getFrequency()).remindersPerDay(p.getRemindersPerDay())
                        .startDate(p.getStartDate()).endDate(p.getEndDate()).instructions(p.getInstructions())
                        .build()).collect(Collectors.toList());
        return PostVisitResponse.builder()
                .id(r.getId()).appointmentId(appointmentId)
                .clinicalNotes(r.getClinicalNotes()).diagnosis(r.getDiagnosis())
                .followUpDate(r.getFollowUpDate()).llmPatientSummary(r.getLlmPatientSummary())
                .llmProcessed(r.getLlmProcessed()).llmError(r.getLlmError())
                .prescriptions(prescriptionDtos).createdAt(r.getCreatedAt())
                .build();
    }

    // ---------------------------------------------------------------
    // Public doctor search
    // ---------------------------------------------------------------

    public List<DoctorResponse> searchBySpecialisation(String specialisation) {
        return doctorRepository.findBySpecialisationContainingIgnoreCase(specialisation)
                .stream().map(adminService::toResponse).collect(Collectors.toList());
    }

    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream().map(adminService::toResponse).collect(Collectors.toList());
    }
}
