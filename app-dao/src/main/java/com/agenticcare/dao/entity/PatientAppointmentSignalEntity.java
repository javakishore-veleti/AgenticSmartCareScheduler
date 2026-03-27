package com.agenticcare.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Real-time signals about a patient's appointment status.
 * Sources: patient SMS link response, provider mobile app, check-in kiosk.
 *
 * Signal types:
 *   PATIENT_ON_MY_WAY     — patient clicked "I'm on my way" (from SMS link)
 *   PATIENT_RUNNING_LATE  — patient clicked "I'll be late"
 *   PATIENT_CANCEL        — patient clicked "Cancel/Reschedule"
 *   PATIENT_CONFIRMED     — patient confirmed via portal/app
 *   PROVIDER_PATIENT_ARRIVED — provider marked patient as arrived
 *   PROVIDER_PATIENT_NOSHOW — provider marked patient as no-show
 *   PROVIDER_OPEN_SLOT    — provider released slot to waitlist
 *   KIOSK_CHECK_IN        — patient checked in at kiosk
 *   OUTREACH_SENT         — outreach was sent to patient (IVR/SMS/Callback)
 *   OUTREACH_SKIPPED      — outreach skipped (patient already confirmed/arriving)
 */
@Entity
@Table(name = "patient_appointment_signal", schema = "smartcare_admin_db")
@Data
@NoArgsConstructor
public class PatientAppointmentSignalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "signal_type", nullable = false)
    private String signalType;

    @Column(name = "signal_source", nullable = false)
    private String signalSource;  // PATIENT_SMS_LINK, PATIENT_APP, PROVIDER_APP, KIOSK, AGENT_PCA, AGENT_COA

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;  // any additional context (estimated arrival, reason for cancel, etc.)

    @Column(name = "workflow_run_id")
    private Long workflowRunId;  // which workflow run generated or consumed this signal

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
