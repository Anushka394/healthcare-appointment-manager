# Healthcare Appointment & Follow-up Manager

A full-stack clinic management platform with separate portals for **patients**, **doctors**, and **admins**. Patients book appointments, submit symptoms (with LLM pre-visit summary), and receive medication reminders. Doctors manage schedules, post-visit notes, and get AI-generated clinical summaries. The system sends email notifications and syncs Google Calendar events for both parties.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Setup Guide](#setup-guide)
4. [API Documentation](#api-documentation)
5. [Database Schema](#database-schema)
6. [LLM Prompts](#llm-prompts)
7. [Google Calendar Setup](#google-calendar-setup)
8. [System Design Write-up](#system-design-write-up)

---

## Tech Stack

| Layer       | Technology                                          |
|-------------|-----------------------------------------------------|
| Backend     | Spring Boot 3.2, Java 17, Spring Security + JWT    |
| Database    | MySQL 8, Hibernate / Spring Data JPA               |
| Scheduler   | Spring `@Scheduled` (Quartz JDBC store)            |
| Email       | JavaMailSender (Gmail SMTP / SendGrid)             |
| LLM         | OpenAI API (OkHttp client, graceful fallback)      |
| Calendar    | Google Calendar API v3, OAuth 2.0                  |
| Frontend    | React 19, Vite, Tailwind CSS 3, React Query, React Router |

---

## Project Structure

```
healthcare-appointment-manager/
├── backend/
│   ├── src/main/java/com/healthcare/
│   │   ├── config/          # SecurityConfig, AppConfig
│   │   ├── controller/      # AuthController, AdminController, DoctorController,
│   │   │                    # PatientController, PublicDoctorController
│   │   ├── dto/             # auth/, doctor/, patient/, appointment/,
│   │   │                    # symptom/, postvisit/, common/
│   │   ├── entity/          # User, Doctor, Patient, Appointment, DoctorLeave,
│   │   │                    # Notification, PostVisitRecord, Prescription,
│   │   │                    # SlotHold, SymptomForm
│   │   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── scheduler/       # 5 background jobs
│   │   ├── security/        # JWT filter, token provider, UserDetailsService
│   │   └── service/         # AuthService, AdminService, DoctorService,
│   │                        # PatientService, AppointmentService,
│   │                        # LlmService, EmailService, GoogleCalendarService,
│   │                        # NotificationService
│   └── src/main/resources/
│       └── application.properties
└── frontend/
    └── src/
        ├── api/             # axios.js + domain api files
        ├── components/      # layout/, common/
        ├── context/         # AuthContext
        └── pages/           # auth/, patient/, doctor/, admin/
```

---

## Setup Guide

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8
- Node.js 18+
- An OpenAI API key
- A Google Cloud project with Calendar API enabled

### 1. Database

```sql
CREATE DATABASE healthcare_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Backend

```bash
cd backend

# Copy and fill in environment variables
cp .env.example .env
# Edit .env with your values

# Load env vars into your shell (PowerShell)
Get-Content .env | ForEach-Object {
  if ($_ -match '^([^#][^=]+)=(.+)$') {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
  }
}

# Run
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**. Hibernate auto-creates all tables on first run (`ddl-auto=update`).

### 3. Frontend

```bash
cd frontend
cp .env.example .env        # Set VITE_API_URL=http://localhost:8080
npm install
npm run dev                 # Starts on http://localhost:5173
```

### 4. Create the first admin user

Insert directly into MySQL (bcrypt hash for password `admin123`):

```sql
INSERT INTO users (name, email, password, role, is_active)
VALUES (
  'Admin',
  'admin@healthcare.com',
  '$2a$12$GvxCGAWDsuIwGD.xS.vCTOKW5dj2R4vHj.cT1SaMvuaJVz2VvN8si',
  'ADMIN',
  true
);
```

---

## API Documentation

### Authentication

| Method | Endpoint             | Auth | Description        |
|--------|----------------------|------|--------------------|
| POST   | /api/auth/register   | None | Register new user  |
| POST   | /api/auth/login      | None | Login, returns JWT |

**Register request:**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "secret123",
  "role": "PATIENT",
  "phoneNumber": "+1234567890"
}
```

**Auth response:**
```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "userId": 1,
  "name": "Jane Doe",
  "email": "jane@example.com",
  "role": "PATIENT",
  "profileId": 1
}
```

---

### Public Endpoints (no auth required)

| Method | Endpoint                                    | Description             |
|--------|---------------------------------------------|-------------------------|
| GET    | /api/doctors/public?specialisation=Cardio   | Search doctors          |
| GET    | /api/doctors/public/{id}                    | Get doctor details      |
| GET    | /api/doctors/public/{id}/slots?date=YYYY-MM-DD | Available time slots |

---

### Patient Endpoints (`PATIENT` role required)

| Method | Endpoint                                         | Description                          |
|--------|--------------------------------------------------|--------------------------------------|
| GET    | /api/patient/profile                             | Get own profile                      |
| PUT    | /api/patient/profile                             | Update profile                       |
| GET    | /api/patient/doctors?specialisation=X            | Search doctors                       |
| GET    | /api/patient/doctors/{id}/slots?date=YYYY-MM-DD  | Get available slots                  |
| POST   | /api/patient/appointments/hold                   | Hold a slot (step 1 of booking)      |
| POST   | /api/patient/appointments/{holdId}/confirm       | Confirm booking (step 2)             |
| POST   | /api/patient/appointments/{id}/symptoms          | Submit symptom form (triggers LLM)   |
| GET    | /api/patient/appointments/{id}/symptoms          | Get symptom form + AI summary        |
| GET    | /api/patient/appointments                        | List all appointments                |
| DELETE | /api/patient/appointments/{id}                   | Cancel appointment                   |
| PUT    | /api/patient/appointments/{id}/reschedule        | Reschedule appointment               |
| GET    | /api/patient/appointments/{id}/post-visit        | Get post-visit summary               |

**Hold slot request:**
```json
{
  "doctorId": 1,
  "appointmentDate": "2026-09-15",
  "slotStartTime": "10:00"
}
```

**Symptom form request:**
```json
{
  "symptoms": "Persistent headache and mild fever for 3 days",
  "symptomDuration": "3 days",
  "severitySelfReported": "Moderate",
  "currentMedications": "Paracetamol 500mg"
}
```

---

### Doctor Endpoints (`DOCTOR` role required)

| Method | Endpoint                                       | Description                     |
|--------|------------------------------------------------|---------------------------------|
| GET    | /api/doctor/profile                            | Get own profile                 |
| PUT    | /api/doctor/profile                            | Update profile                  |
| GET    | /api/doctor/appointments                       | All appointments                |
| GET    | /api/doctor/appointments/date/{date}           | Appointments for a specific day |
| POST   | /api/doctor/appointments/{id}/post-visit       | Submit post-visit notes         |
| GET    | /api/doctor/appointments/{id}/post-visit       | Get post-visit record           |
| POST   | /api/doctor/leaves                             | Request leave                   |
| GET    | /api/doctor/leaves                             | Get own leaves                  |

**Post-visit request:**
```json
{
  "clinicalNotes": "Patient presents with viral URTI. Temp 38.2°C. Throat inflamed.",
  "diagnosis": "Viral Upper Respiratory Tract Infection",
  "followUpDate": "2026-09-29",
  "prescriptions": [
    {
      "medicationName": "Paracetamol",
      "dosage": "500mg",
      "frequency": "Every 6 hours",
      "remindersPerDay": 4,
      "startDate": "2026-09-15",
      "endDate": "2026-09-22",
      "instructions": "Take after food"
    }
  ]
}
```

---

### Admin Endpoints (`ADMIN` role required)

| Method | Endpoint                              | Description                   |
|--------|---------------------------------------|-------------------------------|
| POST   | /api/admin/doctors                    | Create doctor profile         |
| PUT    | /api/admin/doctors/{id}               | Update doctor profile         |
| GET    | /api/admin/doctors                    | List all doctors              |
| GET    | /api/admin/doctors/{id}               | Get doctor by ID              |
| POST   | /api/admin/doctors/{id}/leaves        | Add leave for doctor          |
| GET    | /api/admin/doctors/{id}/leaves        | Get doctor's leaves           |
| DELETE | /api/admin/leaves/{leaveId}           | Remove a leave                |
| GET    | /api/admin/users                      | List all users                |
| PATCH  | /api/admin/users/{id}/activate        | Activate user                 |
| PATCH  | /api/admin/users/{id}/deactivate      | Deactivate user               |

---

## Database Schema

```sql
-- Core user account
users (id, name, email, password, role ENUM(PATIENT,DOCTOR,ADMIN),
       phone_number, is_active,
       google_access_token, google_refresh_token, google_token_expiry,
       created_at, updated_at)

-- Doctor professional profile
doctors (id, user_id FK, specialisation, work_start_time, work_end_time,
         slot_duration_minutes, max_patients_per_day, biography,
         consultation_fee)

-- Patient medical profile
patients (id, user_id FK, date_of_birth, gender ENUM(MALE,FEMALE,OTHER),
          blood_group, allergies,
          emergency_contact_name, emergency_contact_phone)

-- Appointment (UNIQUE: doctor_id + appointment_date + slot_start_time)
appointments (id, patient_id FK, doctor_id FK,
              appointment_date, slot_start_time, slot_end_time,
              status ENUM(SLOT_HELD,PENDING_SYMPTOMS,CONFIRMED,
                          COMPLETED,CANCELLED,CANCELLED_DOCTOR_LEAVE),
              cancellation_reason,
              patient_calendar_event_id, doctor_calendar_event_id,
              reminder_sent, created_at, updated_at)

-- Temporary slot holds during booking flow (expires in N minutes)
slot_holds (id, doctor_id FK, patient_id FK,
            hold_date, slot_start_time,
            expires_at, created_at)
-- UNIQUE: doctor_id + hold_date + slot_start_time

-- Pre-visit symptom form with LLM output
symptom_forms (id, appointment_id FK UNIQUE,
               symptoms, symptom_duration, severity_self_reported,
               current_medications,
               llm_urgency_level, llm_chief_complaint,
               llm_suggested_questions, llm_raw_response,
               llm_processed, llm_error, created_at)

-- Post-visit clinical record with LLM patient summary
post_visit_records (id, appointment_id FK UNIQUE,
                    clinical_notes, diagnosis, follow_up_date,
                    llm_patient_summary, llm_raw_response,
                    llm_processed, llm_error, created_at)

-- Prescriptions (one appointment can have multiple)
prescriptions (id, appointment_id FK, patient_id FK,
               medication_name, dosage, frequency,
               reminders_per_day, start_date, end_date,
               instructions, reminder_active, created_at)

-- Doctor leave days
doctor_leaves (id, doctor_id FK, leave_date, reason,
               patients_notified, created_at)
-- UNIQUE: doctor_id + leave_date

-- Email notification log with retry state
notifications (id, user_id FK, appointment_id FK,
               type ENUM(BOOKING_CONFIRMATION,APPOINTMENT_REMINDER,
                         CANCELLATION,DOCTOR_LEAVE_CANCELLATION,
                         POST_VISIT_SUMMARY,MEDICATION_REMINDER),
               status ENUM(PENDING,SENT,FAILED,EXHAUSTED),
               subject, body TEXT, recipient_email,
               retry_count, max_retries,
               last_attempt_at, sent_at, error_message, created_at)
```

---

## LLM Prompts

### Pre-visit Summary Prompt

```
Analyse these symptoms and return EXACTLY in this format:
URGENCY: <Low|Medium|High>
CHIEF_COMPLAINT: <one sentence>
QUESTION_1: <suggested question for doctor>
QUESTION_2: <suggested question for doctor>
QUESTION_3: <suggested question for doctor>

Symptoms: {symptoms}
```

**Output fields stored in DB:** `llm_urgency_level`, `llm_chief_complaint`, `llm_suggested_questions`

**Failure handling:** If the LLM call times out or returns a non-2xx response, the symptom form is still saved with `llm_processed=false` and `llm_error` populated. The appointment is confirmed regardless — the system never breaks due to LLM failure.

### Post-visit Summary Prompt

```
Convert these clinical notes into a patient-friendly summary with medication
schedule and follow-up steps. Use plain, easy-to-understand language.
Structure it with: Summary, Medications, Follow-up Steps.

Clinical Notes: {clinicalNotes}
```

**Output stored in:** `post_visit_records.llm_patient_summary`

**Failure handling:** Same graceful fallback — the post-visit record is saved with the raw clinical notes even if LLM is unavailable. Patient email falls back to "Summary generation is temporarily unavailable."

---

## Google Calendar Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select an existing one)
3. Enable **Google Calendar API**: APIs & Services → Library → search "Google Calendar API" → Enable
4. Create OAuth 2.0 credentials:
   - APIs & Services → Credentials → Create Credentials → OAuth client ID
   - Application type: **Web application**
   - Authorised redirect URI: `http://localhost:8080/api/calendar/oauth2/callback`
   - Copy the **Client ID** and **Client Secret**
5. Add to your `.env`:
   ```
   GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=your-client-secret
   GOOGLE_REDIRECT_URI=http://localhost:8080/api/calendar/oauth2/callback
   ```
6. Users connect their Google account via the OAuth flow. The access token is stored in `users.google_access_token`. Calendar events are created automatically on booking confirmation and deleted/updated on cancellation/reschedule.

> **Note:** If a user has not connected Google Calendar, the system skips calendar operations silently — bookings still work normally.

---

## System Design Write-up

### Double-Booking Prevention

The system uses a two-layer defence. The first layer is a **slot hold** table with a DB-level unique constraint on `(doctor_id, hold_date, slot_start_time)`. When a patient starts the booking flow, a `SlotHold` record is inserted. If two patients attempt the same slot simultaneously, the second insert throws a constraint violation, which the service catches and converts to a user-friendly `SlotUnavailableException` — no race condition can produce two holds for the same slot.

The second layer activates at booking confirmation: the `AppointmentRepository.findActiveSlotWithLock()` query uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` (a `SELECT ... FOR UPDATE`). This ensures that if two requests somehow pass the hold check, only one can acquire the database row lock and proceed; the other waits and then finds the slot occupied.

Holds expire automatically after 10 minutes. A `SlotHoldCleanupJob` runs every 5 minutes to purge them, releasing slots back to the pool without manual intervention.

### Doctor Leave Conflict Handling

When a doctor is marked on leave, `AdminService.cancelAppointmentsForLeave()` immediately finds all `CONFIRMED` and `PENDING_SYMPTOMS` appointments on that date, sets their status to `CANCELLED_DOCTOR_LEAVE`, and fires a leave-cancellation email to each affected patient. The `DoctorLeave` record carries a `patientsNotified` flag that is set to `true` once all notifications have been dispatched.

As a safety net, `DoctorLeaveConflictJob` runs every 30 minutes. It scans for any leave records where `patientsNotified = false` — which can happen if the AdminService transaction failed partway through — and re-processes them. This guarantees eventual consistency: patients are always notified even if the original transaction partially failed.

### Slot Hold Mechanism

The hold flow mirrors an e-commerce cart checkout pattern:

1. **Hold** (`POST /api/patient/appointments/hold`): A `SlotHold` is created and expires in `SLOT_HOLD_EXPIRY` minutes (default 10). The slot is shown as unavailable in the public slot API during this window.
2. **Fill symptom form**: The patient fills in symptoms. The LLM generates a pre-visit summary asynchronously.
3. **Confirm** (`POST /api/patient/appointments/{holdId}/confirm`): The hold is consumed, a real `Appointment` row is created, and the slot hold is deleted. Email and Google Calendar event are fired after commit so a calendar failure cannot roll back the booking.

If the patient abandons the form, the hold expires and `SlotHoldCleanupJob` reclaims it within 5 minutes.

### Notification Failure Handling

Every outbound email is persisted as a `Notification` record before the send attempt. The entity tracks `status` (PENDING → SENT / FAILED → EXHAUSTED), `retryCount`, and `maxRetries` (default 3). Sending is done asynchronously via `@Async` so a slow SMTP server never blocks the HTTP request.

`NotificationRetryJob` runs every 15 minutes and picks up all `PENDING` or `FAILED` notifications where `retryCount < maxRetries`, then re-attempts delivery. After three failures the record is marked `EXHAUSTED` so operators can query and investigate. This provides at-least-once delivery semantics without requiring an external message queue.

---

## Running the full application

```bash
# Terminal 1 — Backend
cd backend
mvn spring-boot:run

# Terminal 2 — Frontend
cd frontend
npm run dev
```

Open **http://localhost:5173** in your browser.
