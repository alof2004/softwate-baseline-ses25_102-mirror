package org.pt.ua.deti.clinicProject.dto;

import org.pt.ua.deti.clinicProject.models.Appointment;

public record AppointmentSummaryDTO(Long id) {
    public static AppointmentSummaryDTO fromEntity(Appointment a) {
        return new AppointmentSummaryDTO(a.getId());
    }
}
