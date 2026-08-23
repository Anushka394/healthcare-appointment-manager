package com.healthcare.repository;

import com.healthcare.entity.Notification;
import com.healthcare.entity.Notification.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' OR " +
           "(n.status = 'FAILED' AND n.retryCount < n.maxRetries)")
    List<Notification> findPendingAndRetryable();

    @Query("SELECT n FROM Notification n WHERE n.status IN ('PENDING', 'FAILED') " +
           "AND n.retryCount < n.maxRetries ORDER BY n.createdAt ASC")
    List<Notification> findRetryableNotifications();

    List<Notification> findByStatus(NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.appointment.id = :appointmentId")
    List<Notification> findByAppointmentId(@Param("appointmentId") Long appointmentId);
}
