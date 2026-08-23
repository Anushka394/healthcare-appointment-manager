package com.healthcare.repository;

import com.healthcare.entity.Appointment;
import com.healthcare.entity.Appointment.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByAppointmentDateDescSlotStartTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdAndAppointmentDateOrderBySlotStartTime(Long doctorId, LocalDate date);

    List<Appointment> findByDoctorIdOrderByAppointmentDateDescSlotStartTimeDesc(Long doctorId);

    /**
     * Pessimistic write lock to prevent concurrent double-booking on the same slot.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDate = :date AND a.slotStartTime = :slotTime " +
           "AND a.status NOT IN :excludedStatuses")
    Optional<Appointment> findActiveSlotWithLock(@Param("doctorId") Long doctorId,
                                                  @Param("date") LocalDate date,
                                                  @Param("slotTime") LocalTime slotTime,
                                                  @Param("excludedStatuses") List<AppointmentStatus> excludedStatuses);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status NOT IN ('CANCELLED', 'CANCELLED_DOCTOR_LEAVE', 'SLOT_HELD')")
    List<Appointment> findConfirmedByDoctorAndDate(@Param("doctorId") Long doctorId,
                                                    @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDate = :date " +
           "AND a.status IN ('CONFIRMED', 'PENDING_SYMPTOMS')")
    List<Appointment> findActiveByDoctorAndDate(@Param("doctorId") Long doctorId,
                                                 @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date " +
           "AND a.status = 'CONFIRMED' AND a.reminderSent = false")
    List<Appointment> findUnremindedForDate(@Param("date") LocalDate date);

    boolean existsByDoctorIdAndAppointmentDateAndSlotStartTimeAndStatusNotIn(
            Long doctorId, LocalDate date, LocalTime slotTime, List<AppointmentStatus> excludedStatuses);
}
