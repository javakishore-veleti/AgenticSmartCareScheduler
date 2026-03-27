package com.agenticcare.web.controller.customer;

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
 * Customer-facing API for patient appointment signals.
 * Called when patient clicks the SMS link sent 1 hour before appointment.
 *
 * SMS link format: https://portal/api/customer/v1/appointment-signal?p={patientId}&a={appointmentId}&r={response}
 * response: on_my_way | running_late | cancel
 */
@RestController
@RequestMapping("/smart-care/api/customer/v1/appointment-signals")
@Tag(name = "Customer - Appointment Signals", description = "Patient responds to appointment outreach via SMS link")
public class PatientSignalController {

    private static final Logger log = LoggerFactory.getLogger(PatientSignalController.class);
    private final PatientAppointmentSignalRepository repo;

    public PatientSignalController(PatientAppointmentSignalRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/respond")
    @Operation(summary = "Patient clicks SMS link to respond (GET for SMS deep-link compatibility)")
    public ResponseEntity<Map<String, String>> respondViaLink(
            @RequestParam("p") String patientId,
            @RequestParam("a") String appointmentId,
            @RequestParam("r") String response) {

        log.info(">>> Patient signal: patient={} appointment={} response={}", patientId, appointmentId, response);

        String signalType = switch (response.toLowerCase()) {
            case "on_my_way", "coming" -> "PATIENT_ON_MY_WAY";
            case "late", "running_late" -> "PATIENT_RUNNING_LATE";
            case "cancel", "reschedule" -> "PATIENT_CANCEL";
            case "confirmed", "yes" -> "PATIENT_CONFIRMED";
            default -> "PATIENT_UNKNOWN_RESPONSE";
        };

        PatientAppointmentSignalEntity signal = new PatientAppointmentSignalEntity();
        signal.setPatientId(patientId);
        signal.setAppointmentId(appointmentId);
        signal.setSignalType(signalType);
        signal.setSignalSource("PATIENT_SMS_LINK");
        signal.setDetailJson("{\"raw_response\": \"" + response + "\"}");
        repo.save(signal);

        String message = switch (signalType) {
            case "PATIENT_ON_MY_WAY" -> "Thank you! We'll see you soon.";
            case "PATIENT_RUNNING_LATE" -> "Thank you for letting us know. We'll adjust accordingly.";
            case "PATIENT_CANCEL" -> "Your appointment has been noted for cancellation. A staff member may contact you to reschedule.";
            case "PATIENT_CONFIRMED" -> "Your appointment is confirmed. See you soon!";
            default -> "Response received. Thank you.";
        };

        return ResponseEntity.ok(Map.of("status", "received", "message", message, "signalType", signalType));
    }

    @PostMapping
    @Operation(summary = "Post a patient signal (from patient app or portal)")
    public ResponseEntity<Map<String, Object>> postSignal(@RequestBody Map<String, String> req) {
        log.info(">>> POST patient signal: patient={} type={}", req.get("patientId"), req.get("signalType"));

        PatientAppointmentSignalEntity signal = new PatientAppointmentSignalEntity();
        signal.setPatientId(req.get("patientId"));
        signal.setAppointmentId(req.get("appointmentId"));
        signal.setSignalType(req.get("signalType"));
        signal.setSignalSource(req.getOrDefault("signalSource", "PATIENT_APP"));
        signal.setDetailJson(req.get("detailJson"));
        signal = repo.save(signal);

        return ResponseEntity.ok(Map.of("status", "recorded", "id", signal.getId()));
    }
}
