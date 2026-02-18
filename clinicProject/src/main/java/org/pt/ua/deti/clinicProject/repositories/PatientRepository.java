package org.pt.ua.deti.clinicProject.repositories;

import java.util.Optional;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmail(String email);
}
