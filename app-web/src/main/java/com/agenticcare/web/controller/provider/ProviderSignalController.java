package com.agenticcare.web.controller.provider;

import com.agenticcare.dao.entity.PatientAppointmentSignalEntity;
import com.agenticcare.dao.repository.PatientAppointmentSignalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Provider-facing API for appointment events from provider mobile app.
 * Providers can mark patients as arrived, no-show, or release slots.
 */
@RestController
@RequestMapping("/smart-care/api/provider/v1/appointment-signals")
@Tag(name = "Provider - Appointment Signals", description = "Provider app triggers appointment events")
public class ProviderSignalController {

    private static final Logger log = LoggerFactory.getLogger(ProviderSignalController.class);
    private final PatientAppointmentSignalRepository repo;

    public ProviderSignalController(PatientAppointmentSignalRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/patient-arrived")
    @Operation(summary = "Provider marks patient as arrived — cancels any pending outreach")
    public ResponseEntity<Map<String, Object>> patientArrived(@RequestBody Map<String, String> req) {
        log.info(">>> Provider: patient arrived patient={} appointment={}", req.get("patientId"), req.get("appointmentId"));
        return saveSignal(req, "PROVIDER_PATIENT_ARRIVED", "PROVIDER_APP");
    }

    @PostMapping("/patient-noshow")
    @Operation(summary = "Provider marks patient as no-show — triggers immediate outreach or waitlist")
    public ResponseEntity<Map<String, Object>> patientNoShow(@RequestBody Map<String, String> req) {
        log.info(">>> Provider: patient no-show patient={} appointment={}", req.get("patientId"), req.get("appointmentId"));
        return saveSignal(req, "PROVIDER_PATIENT_NOSHOW", "PROVIDER_APP");
    }

    @PostMapping("/open-slot")
    @Operation(summary = "Provider releases slot to waitlist — RRA agent takes over")
    public ResponseEntity<Map<String, Object>> openSlot(@RequestBody Map<String, String> req) {
        log.info(">>> Provider: open slot appointment={}", req.get("appointmentId"));
        return saveSignal(req, "PROVIDER_OPEN_SLOT", "PROVIDER_APP");
    }

    private ResponseEntity<Map<String, Object>> saveSignal(Map<String, String> req, String signalType, String source) {
        PatientAppointmentSignalEntity signal = new PatientAppointmentSignalEntity();
        signal.setPatientId(req.get("patientId"));
        signal.setAppointmentId(req.get("appointmentId"));
        signal.setSignalType(signalType);
        signal.setSignalSource(source);
        signal.setDetailJson(req.get("detailJson"));
        signal = repo.save(signal);
        return ResponseEntity.ok(Map.of("status", "recorded", "id", signal.getId(), "signalType", signalType));
    }
}
