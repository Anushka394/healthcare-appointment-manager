package com.healthcare.service;

import com.healthcare.dto.patient.PatientRequest;
import com.healthcare.dto.patient.PatientResponse;
import com.healthcare.dto.symptom.SymptomFormRequest;
import com.healthcare.dto.symptom.SymptomFormResponse;
import com.healthcare.entity.*;
import com.healthcare.exception.*;
import com.healthcare.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final SymptomFormRepository symptomFormRepository;
    private final LlmService llmService;

    // ---------------------------------------------------------------
    // Profile
    // ---------------------------------------------------------------

    public PatientResponse getMyProfile(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse updateMyProfile(Long userId, PatientRequest req) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        if (req.getDateOfBirth() != null)          patient.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender() != null)               patient.setGender(req.getGender());
        if (req.getBloodGroup() != null)            patient.setBloodGroup(req.getBloodGroup());
        if (req.getAllergies() != null)             patient.setAllergies(req.getAllergies());
        if (req.getEmergencyContactName() != null)  patient.setEmergencyContactName(req.getEmergencyContactName());
        if (req.getEmergencyContactPhone() != null) patient.setEmergencyContactPhone(req.getEmergencyContactPhone());
        return toResponse(patientRepository.save(patient));
    }

    // ---------------------------------------------------------------
    // Symptom form
    // ---------------------------------------------------------------

    @Transactional
    public SymptomFormResponse submitSymptomForm(Long userId, Long appointmentId, SymptomFormRequest req) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        if (!appt.getPatient().getId().equals(patient.getId())) {
            throw new UnauthorizedException("This appointment does not belong to you");
        }

        // Accept any non-completed, non-cancelled status
        Appointment.AppointmentStatus status = appt.getStatus();
        if (status == Appointment.AppointmentStatus.COMPLETED
                || status == Appointment.AppointmentStatus.CANCELLED
                || status == Appointment.AppointmentStatus.CANCELLED_DOCTOR_LEAVE) {
            throw new BadRequestException("Cannot submit symptom form for appointment with status: " + status);
        }

        // If already submitted, just return existing
        if (symptomFormRepository.existsByAppointmentId(appointmentId)) {
            return toSymptomResponse(symptomFormRepository.findByAppointmentId(appointmentId).get());
        }

        // LLM call — graceful fallback, never throws
        LlmService.PreVisitResult llm = llmService.generatePreVisitSummary(req.getSymptoms());

        SymptomForm form = SymptomForm.builder()
                .appointment(appt)
                .symptoms(req.getSymptoms())
                .symptomDuration(req.getSymptomDuration())
                .severitySelfReported(req.getSeveritySelfReported())
                .currentMedications(req.getCurrentMedications())
                .llmUrgencyLevel(llm.getUrgencyLevel())
                .llmChiefComplaint(llm.getChiefComplaint())
                .llmSuggestedQuestions(llm.getSuggestedQuestions())
                .llmRawResponse(llm.getRawResponse())
                .llmProcessed(llm.isSuccess())
                .llmError(llm.getErrorMessage())
                .build();

        form = symptomFormRepository.save(form);

        // Move to CONFIRMED
        appt.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appt);

        log.info("Symptom form saved for appointment {} [urgency={}]", appointmentId, llm.getUrgencyLevel());
        return toSymptomResponse(form);
    }

    public SymptomFormResponse getSymptomForm(Long userId, Long appointmentId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        if (!appt.getPatient().getId().equals(patient.getId())) {
            throw new UnauthorizedException("This appointment does not belong to you");
        }
        SymptomForm form = symptomFormRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Symptom form not found for appointment " + appointmentId));
        return toSymptomResponse(form);
    }

    // ---------------------------------------------------------------
    // Mappers
    // ---------------------------------------------------------------

    public PatientResponse toResponse(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .name(p.getUser().getName())
                .email(p.getUser().getEmail())
                .phoneNumber(p.getUser().getPhoneNumber())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .bloodGroup(p.getBloodGroup())
                .allergies(p.getAllergies())
                .emergencyContactName(p.getEmergencyContactName())
                .emergencyContactPhone(p.getEmergencyContactPhone())
                .build();
    }

    private SymptomFormResponse toSymptomResponse(SymptomForm f) {
        return SymptomFormResponse.builder()
                .id(f.getId())
                .appointmentId(f.getAppointment().getId())
                .symptoms(f.getSymptoms())
                .symptomDuration(f.getSymptomDuration())
                .severitySelfReported(f.getSeveritySelfReported())
                .currentMedications(f.getCurrentMedications())
                .llmUrgencyLevel(f.getLlmUrgencyLevel())
                .llmChiefComplaint(f.getLlmChiefComplaint())
                .llmSuggestedQuestions(f.getLlmSuggestedQuestions())
                .llmProcessed(f.getLlmProcessed())
                .llmError(f.getLlmError())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
