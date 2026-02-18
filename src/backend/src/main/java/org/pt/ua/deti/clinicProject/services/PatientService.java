package org.pt.ua.deti.clinicProject.services;

import java.util.List;
import java.util.Optional;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.pt.ua.deti.clinicProject.repositories.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class PatientService {
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
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

    public boolean delete(Long id) {
        if (!patientRepository.existsById(id)) {
            return false;
        }
        patientRepository.deleteById(id);
        return true;
    }
}
