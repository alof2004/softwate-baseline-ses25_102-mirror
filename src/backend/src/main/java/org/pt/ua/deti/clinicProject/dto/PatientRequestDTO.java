package org.pt.ua.deti.clinicProject.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record PatientRequestDTO(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\+?[0-9 \\-]{7,20}", message = "Invalid phone number format")
        String phoneNumber,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        String email) {}
