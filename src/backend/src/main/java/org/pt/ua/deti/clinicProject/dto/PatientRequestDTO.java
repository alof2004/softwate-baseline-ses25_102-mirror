package org.pt.ua.deti.clinicProject.dto;

import java.time.LocalDate;

public record PatientRequestDTO(
        String name,
        LocalDate dateOfBirth,
        String phoneNumber,
        String email) {}
