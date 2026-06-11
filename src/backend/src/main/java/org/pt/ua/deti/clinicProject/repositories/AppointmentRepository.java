package org.pt.ua.deti.clinicProject.repositories;

import java.util.List;
import org.pt.ua.deti.clinicProject.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {
    List<Appointment> findByPatientId(Long patientId);
}
