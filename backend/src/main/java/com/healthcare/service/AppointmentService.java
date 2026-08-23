package com.healthcare.service;

import com.healthcare.dto.appointment.*;
import com.healthcare.entity.*;
import com.healthcare.entity.Appointment.AppointmentStatus;
import com.healthcare.exception.*;
import com.healthcare.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SlotHoldRepository slotHoldRepository;
    private final DoctorLeaveRepository doctorLeaveRepository;
    private final EmailService emailService;
    private final GoogleCalendarService calendarService;

    @Value("${app.scheduler.slot-hold-expiry-minutes:10}")
    private int slotHoldExpiryMinutes;

    // ---------------------------------------------------------------
    // Available slots
    // ---------------------------------------------------------------

    public AvailableSlotsResponse getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        if (doctorLeaveRepository.existsByDoctorIdAndLeaveDate(doctorId, date)) {
            return AvailableSlotsResponse.builder()
                    .doctorId(doctorId).date(date).availableSlots(List.of()).build();
        }

        // All slots for the day
        List<LocalTime> allSlots = generateSlots(
                doctor.getWorkStartTime(), doctor.getWorkEndTime(), doctor.getSlotDurationMinutes());

        // Booked slots
        Set<LocalTime> bookedTimes = appointmentRepository
                .findConfirmedByDoctorAndDate(doctorId, date)
                .stream().map(Appointment::getSlotStartTime).collect(Collectors.toSet());

        // Held slots
        Set<LocalTime> heldTimes = slotHoldRepository
                .findActiveHoldsByDoctorAndDate(doctorId, date, LocalDateTime.now())
                .stream().map(SlotHold::getSlotStartTime).collect(Collectors.toSet());

        List<AvailableSlotsResponse.SlotInfo> slotInfos = new ArrayList<>();
        for (LocalTime start : allSlots) {
            LocalTime end = start.plusMinutes(doctor.getSlotDurationMinutes());
            boolean available = !bookedTimes.contains(start) && !heldTimes.contains(start);
            slotInfos.add(AvailableSlotsResponse.SlotInfo.builder()
                    .startTime(start).endTime(end).available(available).build());
        }

        return AvailableSlotsResponse.builder()
                .doctorId(doctorId).date(date).availableSlots(slotInfos).build();
    }

    // ---------------------------------------------------------------
    // Slot hold — step 1 of booking flow
    // ---------------------------------------------------------------

    /**
     * Acquires a temporary hold on a slot (max {@code slotHoldExpiryMinutes}).
     * Uses a DB-level unique constraint on (doctor_id, hold_date, slot_start_time)
     * to prevent two patients from holding the same slot simultaneously.
     */
    @Transactional
    public SlotHold holdSlot(Long patientUserId, SlotHoldRequest req) {
        Doctor doctor = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", req.getDoctorId()));

        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for current user"));

        // Doctor on leave?
        if (doctorLeaveRepository.existsByDoctorIdAndLeaveDate(req.getDoctorId(), req.getAppointmentDate())) {
            throw new SlotUnavailableException("Doctor is on leave on " + req.getAppointmentDate());
        }

        // Already confirmed/pending appointment for this slot?
        boolean slotTaken = appointmentRepository.existsByDoctorIdAndAppointmentDateAndSlotStartTimeAndStatusNotIn(
                req.getDoctorId(), req.getAppointmentDate(), req.getSlotStartTime(),
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.CANCELLED_DOCTOR_LEAVE,
                        AppointmentStatus.SLOT_HELD));
        if (slotTaken) throw new SlotUnavailableException("This slot is already booked");

        // Active hold by another patient?
        if (slotHoldRepository.existsActiveHold(req.getDoctorId(), req.getAppointmentDate(),
                req.getSlotStartTime(), LocalDateTime.now())) {
            throw new SlotUnavailableException("This slot is temporarily held by another patient");
        }

        // Release any previous hold by this patient
        slotHoldRepository.findActiveHoldByPatient(patient.getId(), LocalDateTime.now())
                .ifPresent(slotHoldRepository::delete);

        SlotHold hold = SlotHold.builder()
                .doctor(doctor)
                .patient(patient)
                .holdDate(req.getAppointmentDate())
                .slotStartTime(req.getSlotStartTime())
                .expiresAt(LocalDateTime.now().plusMinutes(slotHoldExpiryMinutes))
                .build();

        try {
            return slotHoldRepository.save(hold);
        } catch (Exception e) {
            // Unique constraint violation — another concurrent request grabbed it first
            throw new SlotUnavailableException("This slot was just taken. Please choose another.");
        }
    }

    // ---------------------------------------------------------------
    // Confirm booking — step 2, after symptom form is submitted
    // ---------------------------------------------------------------

    /**
     * Converts a slot hold into a confirmed Appointment.
     * Uses PESSIMISTIC_WRITE lock on the target slot to guarantee no double-booking.
     */
    @Transactional
    public Appointment confirmBooking(Long patientUserId, Long holdId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        SlotHold hold = slotHoldRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot hold", holdId));

        if (!hold.getPatient().getId().equals(patient.getId())) {
            throw new UnauthorizedException("This slot hold does not belong to you");
        }
        if (hold.getExpiresAt().isBefore(LocalDateTime.now())) {
            slotHoldRepository.delete(hold);
            throw new SlotUnavailableException("Your slot hold has expired. Please start over.");
        }

        // Final double-booking guard with pessimistic lock
        appointmentRepository.findActiveSlotWithLock(
                hold.getDoctor().getId(), hold.getHoldDate(), hold.getSlotStartTime())
                .ifPresent(a -> { throw new SlotUnavailableException("Slot was just booked by someone else"); });

        Doctor doctor = hold.getDoctor();
        LocalTime endTime = hold.getSlotStartTime().plusMinutes(doctor.getSlotDurationMinutes());

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(hold.getHoldDate())
                .slotStartTime(hold.getSlotStartTime())
                .slotEndTime(endTime)
                .status(AppointmentStatus.PENDING_SYMPTOMS)
                .build();

        appointment = appointmentRepository.save(appointment);
        slotHoldRepository.delete(hold);

        // Fire-and-forget: email + calendar (failures must not roll back the booking)
        try {
            emailService.sendBookingConfirmation(appointment);
        } catch (Exception e) {
            log.error("Booking confirmation email failed for appointment {}: {}", appointment.getId(), e.getMessage());
        }
        try {
            calendarService.createAppointmentEvent(appointment);
            appointmentRepository.save(appointment); // persist calendar event IDs
        } catch (Exception e) {
            log.error("Calendar event creation failed for appointment {}: {}", appointment.getId(), e.getMessage());
        }

        log.info("Appointment {} confirmed for patient {} with doctor {} on {}",
                appointment.getId(), patient.getId(), doctor.getId(), appointment.getAppointmentDate());
        return appointment;
    }

    // ---------------------------------------------------------------
    // Cancel
    // ---------------------------------------------------------------

    @Transactional
    public Appointment cancel(Long appointmentId, Long requestingUserId, String reason) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        // Only patient, the appointment's doctor, or admin can cancel
        boolean isPatient = appt.getPatient().getUser().getId().equals(requestingUserId);
        boolean isDoctor = appt.getDoctor().getUser().getId().equals(requestingUserId);
        if (!isPatient && !isDoctor) {
            throw new UnauthorizedException("You are not authorised to cancel this appointment");
        }
        if (appt.getStatus() == AppointmentStatus.COMPLETED
                || appt.getStatus() == AppointmentStatus.CANCELLED
                || appt.getStatus() == AppointmentStatus.CANCELLED_DOCTOR_LEAVE) {
            throw new BadRequestException("Appointment is already " + appt.getStatus());
        }

        appt.setStatus(AppointmentStatus.CANCELLED);
        appt.setCancellationReason(reason);
        appt = appointmentRepository.save(appt);

        try { emailService.sendCancellationNotice(appt, reason); } catch (Exception e) {
            log.error("Cancellation email failed: {}", e.getMessage());
        }
        try { calendarService.deleteAppointmentEvent(appt); } catch (Exception e) {
            log.error("Calendar delete failed: {}", e.getMessage());
        }
        return appt;
    }

    // ---------------------------------------------------------------
    // Reschedule
    // ---------------------------------------------------------------

    @Transactional
    public Appointment reschedule(Long appointmentId, Long patientUserId, RescheduleRequest req) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        if (!appt.getPatient().getUser().getId().equals(patientUserId)) {
            throw new UnauthorizedException("You can only reschedule your own appointments");
        }
        if (appt.getStatus() == AppointmentStatus.COMPLETED
                || appt.getStatus() == AppointmentStatus.CANCELLED
                || appt.getStatus() == AppointmentStatus.CANCELLED_DOCTOR_LEAVE) {
            throw new BadRequestException("Cannot reschedule an appointment with status: " + appt.getStatus());
        }

        // Check new slot availability
        if (doctorLeaveRepository.existsByDoctorIdAndLeaveDate(appt.getDoctor().getId(), req.getNewDate())) {
            throw new SlotUnavailableException("Doctor is on leave on " + req.getNewDate());
        }
        appointmentRepository.findActiveSlotWithLock(
                appt.getDoctor().getId(), req.getNewDate(), req.getNewSlotStartTime())
                .ifPresent(a -> { throw new SlotUnavailableException("The new slot is already booked"); });

        LocalTime newEnd = req.getNewSlotStartTime().plusMinutes(appt.getDoctor().getSlotDurationMinutes());

        appt.setAppointmentDate(req.getNewDate());
        appt.setSlotStartTime(req.getNewSlotStartTime());
        appt.setSlotEndTime(newEnd);
        appt = appointmentRepository.save(appt);

        try { emailService.sendBookingConfirmation(appt); } catch (Exception e) {
            log.error("Reschedule email failed: {}", e.getMessage());
        }
        try { calendarService.updateAppointmentEvent(appt); } catch (Exception e) {
            log.error("Calendar update failed: {}", e.getMessage());
        }
        return appt;
    }

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------

    public List<Appointment> getAppointmentsForPatient(Long patientUserId) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescSlotStartTimeDesc(patient.getId());
    }

    public List<Appointment> getAppointmentsForDoctor(Long doctorUserId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDescSlotStartTimeDesc(doctor.getId());
    }

    public Appointment getById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private List<LocalTime> generateSlots(LocalTime start, LocalTime end, int durationMinutes) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime current = start;
        while (!current.plusMinutes(durationMinutes).isAfter(end)) {
            slots.add(current);
            current = current.plusMinutes(durationMinutes);
        }
        return slots;
    }

    public AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getUser().getName())
                .patientEmail(a.getPatient().getUser().getEmail())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getUser().getName())
                .doctorSpecialisation(a.getDoctor().getSpecialisation())
                .appointmentDate(a.getAppointmentDate())
                .slotStartTime(a.getSlotStartTime())
                .slotEndTime(a.getSlotEndTime())
                .status(a.getStatus())
                .cancellationReason(a.getCancellationReason())
                .reminderSent(a.getReminderSent())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
