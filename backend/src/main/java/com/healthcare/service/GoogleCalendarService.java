package com.healthcare.service;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.healthcare.entity.Appointment;
import com.healthcare.entity.User;
import com.healthcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages Google Calendar events for appointments.
 *
 * Each user stores their own OAuth tokens in the users table
 * (googleAccessToken / googleRefreshToken). When a token is absent
 * the method silently skips — the booking flow is never blocked by
 * calendar failures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private static final String CALENDAR_ID = "primary";
    private static final String APP_NAME = "Healthcare Appointment Manager";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

    private final UserRepository userRepository;

    @Value("${app.google.client-id}")
    private String clientId;

    @Value("${app.google.client-secret}")
    private String clientSecret;

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Creates calendar events for both the patient and doctor.
     * Returns immediately if either user has no Google token.
     */
    public void createAppointmentEvent(Appointment appointment) {
        Event event = buildEvent(appointment);

        createEventForUser(appointment.getPatient().getUser(), event, appointment);
        createEventForUser(appointment.getDoctor().getUser(), event, appointment);
    }

    /**
     * Updates an existing event when an appointment is rescheduled.
     */
    public void updateAppointmentEvent(Appointment appointment) {
        Event event = buildEvent(appointment);

        updateEventForUser(appointment.getPatient().getUser(),
                appointment.getPatientCalendarEventId(), event);
        updateEventForUser(appointment.getDoctor().getUser(),
                appointment.getDoctorCalendarEventId(), event);
    }

    /**
     * Deletes the calendar events when an appointment is cancelled.
     */
    public void deleteAppointmentEvent(Appointment appointment) {
        deleteEventForUser(appointment.getPatient().getUser(),
                appointment.getPatientCalendarEventId());
        deleteEventForUser(appointment.getDoctor().getUser(),
                appointment.getDoctorCalendarEventId());
    }

    // ---------------------------------------------------------------
    // Per-user operations (all failures are swallowed with a log)
    // ---------------------------------------------------------------

    private void createEventForUser(User user, Event event, Appointment appointment) {
        try {
            Calendar service = buildCalendarService(user);
            if (service == null) return;

            Event created = service.events().insert(CALENDAR_ID, event).execute();
            // Persist the event ID back to the appointment
            if (user.getId().equals(appointment.getPatient().getUser().getId())) {
                appointment.setPatientCalendarEventId(created.getId());
            } else {
                appointment.setDoctorCalendarEventId(created.getId());
            }
            log.info("Calendar event created for user {} [eventId={}]", user.getEmail(), created.getId());
        } catch (Exception e) {
            log.warn("Could not create calendar event for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void updateEventForUser(User user, String eventId, Event updatedEvent) {
        if (eventId == null || eventId.isBlank()) return;
        try {
            Calendar service = buildCalendarService(user);
            if (service == null) return;
            service.events().update(CALENDAR_ID, eventId, updatedEvent).execute();
            log.info("Calendar event updated for user {} [eventId={}]", user.getEmail(), eventId);
        } catch (Exception e) {
            log.warn("Could not update calendar event for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void deleteEventForUser(User user, String eventId) {
        if (eventId == null || eventId.isBlank()) return;
        try {
            Calendar service = buildCalendarService(user);
            if (service == null) return;
            service.events().delete(CALENDAR_ID, eventId).execute();
            log.info("Calendar event deleted for user {} [eventId={}]", user.getEmail(), eventId);
        } catch (Exception e) {
            log.warn("Could not delete calendar event for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Build helpers
    // ---------------------------------------------------------------

    private Event buildEvent(Appointment appointment) {
        String title = "Medical Appointment – Dr. " + appointment.getDoctor().getUser().getName();
        String description = "Patient: " + appointment.getPatient().getUser().getName() +
                "\nSpecialisation: " + appointment.getDoctor().getSpecialisation() +
                "\nDate: " + appointment.getAppointmentDate() +
                "\nTime: " + appointment.getSlotStartTime() + " – " + appointment.getSlotEndTime();

        LocalDateTime startDt = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getSlotStartTime());
        LocalDateTime endDt = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getSlotEndTime());

        EventDateTime start = new EventDateTime()
                .setDateTime(toGoogleDateTime(startDt))
                .setTimeZone(DEFAULT_ZONE.getId());
        EventDateTime end = new EventDateTime()
                .setDateTime(toGoogleDateTime(endDt))
                .setTimeZone(DEFAULT_ZONE.getId());

        return new Event()
                .setSummary(title)
                .setDescription(description)
                .setStart(start)
                .setEnd(end)
                .setReminders(new Event.Reminders()
                        .setUseDefault(false)
                        .setOverrides(List.of(
                                new EventReminder().setMethod("email").setMinutes(60),
                                new EventReminder().setMethod("popup").setMinutes(30)
                        )));
    }

    private Calendar buildCalendarService(User user) {
        if (user.getGoogleAccessToken() == null || user.getGoogleAccessToken().isBlank()) {
            log.debug("No Google token for user {}, skipping calendar operation", user.getEmail());
            return null;
        }
        try {
            Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                    .setAccessToken(user.getGoogleAccessToken());

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(APP_NAME)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to build Calendar service for {}: {}", user.getEmail(), e.getMessage());
            return null;
        }
    }

    private DateTime toGoogleDateTime(LocalDateTime ldt) {
        long epochMillis = ldt.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
        return new DateTime(epochMillis);
    }

    // ---------------------------------------------------------------
    // OAuth helpers — store tokens on the user record
    // ---------------------------------------------------------------

    public void saveTokens(Long userId, String accessToken, String refreshToken,
                           LocalDateTime expiry) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setGoogleAccessToken(accessToken);
            user.setGoogleRefreshToken(refreshToken);
            user.setGoogleTokenExpiry(expiry);
            userRepository.save(user);
        });
    }
}
