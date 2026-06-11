package org.pt.ua.deti.clinicProject.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.pt.ua.deti.clinicProject.models.Appointment;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.pt.ua.deti.clinicProject.repositories.AppointmentRepository;
import org.pt.ua.deti.clinicProject.repositories.PatientRepository;
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
        return patientRepository.findAll();
    }

    public Optional<Patient> getById(Long id) {
        return patientRepository.findById(id);
    }

    public Patient create(Patient patient) {
        patient.setId(null);
        return patientRepository.save(patient);
    }

    public Optional<Patient> update(Long id, Patient patient) {
        return patientRepository
                .findById(id)
                .map(
                        existing -> {
                            existing.setName(patient.getName());
                            existing.setDateOfBirth(patient.getDateOfBirth());
                            existing.setPhoneNumber(patient.getPhoneNumber());
                            existing.setEmail(patient.getEmail());
                            return patientRepository.save(existing);
                        });
    }

    @Transactional
    public boolean delete(Long id) {
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
}
