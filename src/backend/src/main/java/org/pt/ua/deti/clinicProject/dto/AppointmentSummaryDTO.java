package org.pt.ua.deti.clinicProject.dto;

import java.util.UUID;
import org.pt.ua.deti.clinicProject.models.Appointment;

public record AppointmentSummaryDTO(UUID id) {
    public static AppointmentSummaryDTO fromEntity(Appointment a) {
        return new AppointmentSummaryDTO(a.getId());
    }
}
