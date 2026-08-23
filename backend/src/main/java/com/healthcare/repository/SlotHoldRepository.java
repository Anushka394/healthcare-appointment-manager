package com.healthcare.repository;

import com.healthcare.entity.SlotHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotHoldRepository extends JpaRepository<SlotHold, Long> {

    Optional<SlotHold> findByDoctorIdAndHoldDateAndSlotStartTime(Long doctorId, LocalDate date, LocalTime slotStartTime);

    List<SlotHold> findByPatientId(Long patientId);

    @Query("SELECT sh FROM SlotHold sh WHERE sh.doctor.id = :doctorId " +
           "AND sh.holdDate = :date AND sh.expiresAt > :now")
    List<SlotHold> findActiveHoldsByDoctorAndDate(@Param("doctorId") Long doctorId,
                                                   @Param("date") LocalDate date,
                                                   @Param("now") LocalDateTime now);

    @Query("SELECT sh FROM SlotHold sh WHERE sh.patient.id = :patientId AND sh.expiresAt > :now")
    Optional<SlotHold> findActiveHoldByPatient(@Param("patientId") Long patientId,
                                                @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM SlotHold sh WHERE sh.expiresAt <= :now")
    int deleteExpiredHolds(@Param("now") LocalDateTime now);

    @Query("SELECT CASE WHEN COUNT(sh) > 0 THEN true ELSE false END FROM SlotHold sh " +
           "WHERE sh.doctor.id = :doctorId AND sh.holdDate = :date " +
           "AND sh.slotStartTime = :slotTime AND sh.expiresAt > :now")
    boolean existsActiveHold(@Param("doctorId") Long doctorId,
                              @Param("date") LocalDate date,
                              @Param("slotTime") LocalTime slotTime,
                              @Param("now") LocalDateTime now);
}
