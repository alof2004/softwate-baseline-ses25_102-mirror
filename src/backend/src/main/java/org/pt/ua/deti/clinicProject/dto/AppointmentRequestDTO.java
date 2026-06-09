package org.pt.ua.deti.clinicProject.dto;

import java.time.LocalDateTime;

public record AppointmentRequestDTO(
        LocalDateTime dateTime,
        String specialty,
        String status) {}
