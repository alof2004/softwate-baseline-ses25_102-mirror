package org.pt.ua.deti.clinicProject.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.pt.ua.deti.clinicProject.models.Appointment;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.pt.ua.deti.clinicProject.repositories.AppointmentRepository;
import org.pt.ua.deti.clinicProject.repositories.PatientRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository, PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    public List<Appointment> getAll() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Optional<Appointment> create(Long patientId, Appointment appointment) {
        return patientRepository
                .findById(patientId)
                .map(
                        patient -> {
                            appointment.setId(null);
                            appointment.setPatient(patient);
                            return appointmentRepository.save(appointment);
                        });
    }

    public Optional<Appointment> update(Long id, Appointment appointment) {
        return appointmentRepository
                .findById(id)
                .flatMap(
                        existing -> {
                            existing.setDateTime(appointment.getDateTime());
                            existing.setSpecialty(appointment.getSpecialty());
                            existing.setStatus(appointment.getStatus());
                            if (appointment.getPatient() != null
                                    && appointment.getPatient().getId() != null) {
                                Optional<Patient> patient =
                                        patientRepository.findById(
                                                appointment.getPatient().getId());
                                if (patient.isEmpty()) {
                                    return Optional.empty();
                                }
                                existing.setPatient(patient.get());
                            }
                            return Optional.of(appointmentRepository.save(existing));
                        });
    }

    public boolean delete(Long id) {
        if (!appointmentRepository.existsById(id)) {
            return false;
        }
        appointmentRepository.deleteById(id);
        return true;
    }

    public List<Appointment> search(
            String patientName, LocalDate date, String status, String specialty) {
        Specification<Appointment> spec = (root, query, cb) -> cb.conjunction();

        if (patientName != null && !patientName.isBlank()) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.like(
                                            cb.lower(root.get("patient").get("name")),
                                            "%" + patientName.toLowerCase() + "%"));
        }
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.between(root.get("dateTime"), start, end));
        }
        if (status != null && !status.isBlank()) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }
        if (specialty != null && !specialty.isBlank()) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.equal(
                                            cb.lower(root.get("specialty")),
                                            specialty.toLowerCase()));
        }

        return appointmentRepository.findAll(spec);
    }
}
