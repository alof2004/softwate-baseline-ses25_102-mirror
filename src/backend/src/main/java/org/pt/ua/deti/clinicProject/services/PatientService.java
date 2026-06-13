package org.pt.ua.deti.clinicProject.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.pt.ua.deti.clinicProject.models.Appointment;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.pt.ua.deti.clinicProject.repositories.AppointmentRepository;
import org.pt.ua.deti.clinicProject.repositories.PatientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public PatientService(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<Patient> getAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isDoctor(auth)) {
            return patientRepository.findByDoctorSub(auth.getName());
        }
        return patientRepository.findAll();
    }

    public Optional<Patient> getById(UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return patientRepository.findById(id)
                .filter(p -> canAccess(auth, p.getId()));
    }

    public Patient create(Patient patient) {
        patient.setId(null);
        return patientRepository.save(patient);
    }

    public Optional<Patient> update(UUID id, Patient patient) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return patientRepository.findById(id)
                .filter(p -> canAccess(auth, p.getId()))
                .map(existing -> {
                    existing.setName(patient.getName());
                    existing.setDateOfBirth(patient.getDateOfBirth());
                    existing.setPhoneNumber(patient.getPhoneNumber());
                    existing.setEmail(patient.getEmail());
                    return patientRepository.save(existing);
                });
    }

    @Transactional
    public boolean delete(UUID id) {
        return patientRepository.findById(id).map(patient -> {
            LocalDateTime now = LocalDateTime.now();
            List<Appointment> appointments = appointmentRepository.findByPatientId(id);
            appointments.forEach(a -> a.setDeletedAt(now));
            appointmentRepository.saveAll(appointments);
            patient.setDeletedAt(now);
            patientRepository.save(patient);
            return true;
        }).orElse(false);
    }

    private boolean isDoctor(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
    }

    private boolean canAccess(Authentication auth, UUID patientId) {
        if (!isDoctor(auth)) return true;
        return appointmentRepository.countByPatientIdAndDoctorSub(patientId, auth.getName()) > 0;
    }
}
