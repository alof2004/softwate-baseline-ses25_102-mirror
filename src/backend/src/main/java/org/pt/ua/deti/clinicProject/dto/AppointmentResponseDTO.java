package org.pt.ua.deti.clinicProject.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.pt.ua.deti.clinicProject.models.Appointment;

public record AppointmentResponseDTO(
        UUID id,
        LocalDateTime dateTime,
        String specialty,
        String status,
        UUID patientId,
        String patientName,
        String doctorSub) {

    public static AppointmentResponseDTO fromEntity(Appointment a) {
        UUID pid = a.getPatient() != null ? a.getPatient().getId() : null;
        String pname = a.getPatient() != null ? a.getPatient().getName() : null;
        return new AppointmentResponseDTO(
                a.getId(), a.getDateTime(), a.getSpecialty(), a.getStatus(), pid, pname,
                a.getDoctorSub());
    }
}
