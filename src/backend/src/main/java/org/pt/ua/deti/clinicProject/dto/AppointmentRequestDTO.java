package org.pt.ua.deti.clinicProject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AppointmentRequestDTO(
        @NotNull(message = "Date and time is required")
        LocalDateTime dateTime,

        @NotBlank(message = "Specialty is required")
        String specialty,

        @NotBlank(message = "Status is required")
        String status,

        String doctorSub) {}
