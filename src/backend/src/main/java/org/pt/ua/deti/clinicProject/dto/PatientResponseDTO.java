package org.pt.ua.deti.clinicProject.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.pt.ua.deti.clinicProject.models.Patient;

public record PatientResponseDTO(
        UUID id,
        String name,
        LocalDate dateOfBirth,
        String phoneNumber,
        String email,
        List<AppointmentSummaryDTO> appointments) {

    public static PatientResponseDTO fromEntity(Patient p) {
        var appts = p.getAppointments() == null
                ? List.<AppointmentSummaryDTO>of()
                : p.getAppointments().stream().map(AppointmentSummaryDTO::fromEntity).toList();
        return new PatientResponseDTO(
                p.getId(), p.getName(), p.getDateOfBirth(),
                p.getPhoneNumber(), p.getEmail(), appts);
    }
}
