package com.healthcare.repository;

import com.healthcare.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByAppointmentId(Long appointmentId);

    List<Prescription> findByPatientId(Long patientId);

    @Query("SELECT p FROM Prescription p WHERE p.reminderActive = true " +
           "AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<Prescription> findActivePrescriptionsForToday(@Param("today") LocalDate today);

    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId " +
           "AND p.reminderActive = true AND p.startDate <= :today " +
           "AND (p.endDate IS NULL OR p.endDate >= :today)")
    List<Prescription> findActiveByPatientId(@Param("patientId") Long patientId,
                                              @Param("today") LocalDate today);
}
