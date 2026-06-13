package org.pt.ua.deti.clinicProject.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByEmail(String email);

    @Query("SELECT DISTINCT p FROM Patient p JOIN p.appointments a WHERE a.doctorSub = :doctorSub")
    List<Patient> findByDoctorSub(@Param("doctorSub") String doctorSub);
}
