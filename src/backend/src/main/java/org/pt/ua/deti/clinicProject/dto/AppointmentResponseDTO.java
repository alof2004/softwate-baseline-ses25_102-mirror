package org.pt.ua.deti.clinicProject.dto;

import java.time.LocalDateTime;
import org.pt.ua.deti.clinicProject.models.Appointment;

public record AppointmentResponseDTO(
        Long id,
        LocalDateTime dateTime,
        String specialty,
        String status,
        Long patientId,
        String patientName,
        String doctorSub) {

    public static AppointmentResponseDTO fromEntity(Appointment a) {
        Long pid = a.getPatient() != null ? a.getPatient().getId() : null;
        String pname = a.getPatient() != null ? a.getPatient().getName() : null;
        return new AppointmentResponseDTO(
                a.getId(), a.getDateTime(), a.getSpecialty(), a.getStatus(), pid, pname,
                a.getDoctorSub());
    }
}
