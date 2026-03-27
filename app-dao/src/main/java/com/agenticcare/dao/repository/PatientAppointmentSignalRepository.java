package com.agenticcare.dao.repository;

import com.agenticcare.dao.entity.PatientAppointmentSignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientAppointmentSignalRepository extends JpaRepository<PatientAppointmentSignalEntity, Long> {
    List<PatientAppointmentSignalEntity> findByPatientIdOrderByCreatedAtDesc(String patientId);
    List<PatientAppointmentSignalEntity> findByAppointmentIdOrderByCreatedAtDesc(String appointmentId);
    List<PatientAppointmentSignalEntity> findByPatientIdAndAppointmentIdOrderByCreatedAtDesc(String patientId, String appointmentId);
    Optional<PatientAppointmentSignalEntity> findFirstByPatientIdAndSignalTypeOrderByCreatedAtDesc(String patientId, String signalType);
}
