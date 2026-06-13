package org.pt.ua.deti.clinicProject.repositories;

import java.util.List;
import java.util.UUID;
import org.pt.ua.deti.clinicProject.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AppointmentRepository
        extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {
    List<Appointment> findByPatientId(UUID patientId);
    List<Appointment> findByDoctorSub(String doctorSub);
    long countByPatientIdAndDoctorSub(UUID patientId, String doctorSub);
}
