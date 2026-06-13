package org.pt.ua.deti.clinicProject.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.pt.ua.deti.clinicProject.models.Appointment;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.pt.ua.deti.clinicProject.repositories.AppointmentRepository;
import org.pt.ua.deti.clinicProject.repositories.PatientRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isDoctor(auth)) {
            return appointmentRepository.findByDoctorSub(auth.getName());
        }
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getById(UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return appointmentRepository.findById(id)
                .filter(a -> canAccess(auth, a));
    }

    public Optional<Appointment> create(UUID patientId, Appointment appointment) {
        return patientRepository
                .findById(patientId)
                .map(patient -> {
                    appointment.setId(null);
                    appointment.setPatient(patient);
                    return appointmentRepository.save(appointment);
                });
    }

    public Optional<Appointment> update(UUID id, Appointment appointment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return appointmentRepository.findById(id)
                .filter(a -> canAccess(auth, a))
                .flatMap(existing -> {
                    existing.setDateTime(appointment.getDateTime());
                    existing.setSpecialty(appointment.getSpecialty());
                    existing.setStatus(appointment.getStatus());
                    if (appointment.getPatient() != null
                            && appointment.getPatient().getId() != null) {
                        Optional<Patient> patient =
                                patientRepository.findById(appointment.getPatient().getId());
                        if (patient.isEmpty()) {
                            return Optional.empty();
                        }
                        existing.setPatient(patient.get());
                    }
                    return Optional.of(appointmentRepository.save(existing));
                });
    }

    public boolean delete(UUID id) {
        return appointmentRepository.findById(id).map(appointment -> {
            appointment.setDeletedAt(LocalDateTime.now());
            appointmentRepository.save(appointment);
            return true;
        }).orElse(false);
    }

    public List<Appointment> search(
            String patientName, LocalDate date, String status, String specialty) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Specification<Appointment> spec = (root, query, cb) -> cb.conjunction();

        if (isDoctor(auth)) {
            String sub = auth.getName();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("doctorSub"), sub));
        }
        if (patientName != null && !patientName.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("patient").get("name")),
                            "%" + patientName.toLowerCase() + "%"));
        }
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.between(root.get("dateTime"), start, end));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }
        if (specialty != null && !specialty.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("specialty")), specialty.toLowerCase()));
        }

        return appointmentRepository.findAll(spec);
    }

    private boolean isDoctor(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
    }

    private boolean canAccess(Authentication auth, Appointment appointment) {
        if (!isDoctor(auth)) return true;
        return auth.getName().equals(appointment.getDoctorSub());
    }
}
