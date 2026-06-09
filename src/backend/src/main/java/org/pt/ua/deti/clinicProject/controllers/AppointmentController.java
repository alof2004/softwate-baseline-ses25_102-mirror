package org.pt.ua.deti.clinicProject.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.pt.ua.deti.clinicProject.dto.AppointmentRequestDTO;
import org.pt.ua.deti.clinicProject.dto.AppointmentResponseDTO;
import org.pt.ua.deti.clinicProject.models.Appointment;
import org.pt.ua.deti.clinicProject.services.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Appointments", description = "Manage clinic appointments")
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Operation(summary = "List all appointments, optionally filtered")
    @GetMapping
    public List<AppointmentResponseDTO> getAll(
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String specialty) {
        List<Appointment> results = (patientName == null && date == null && status == null && specialty == null)
                ? appointmentService.getAll()
                : appointmentService.search(patientName, date, status, specialty);
        return results.stream().map(AppointmentResponseDTO::fromEntity).toList();
    }

    @Operation(summary = "Get appointment by ID")
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getById(@PathVariable Long id) {
        return appointmentService.getById(id)
                .map(AppointmentResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new appointment for a patient")
    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> create(
            @RequestParam Long patientId, @RequestBody AppointmentRequestDTO dto) {
        Appointment a = new Appointment();
        a.setDateTime(dto.dateTime());
        a.setSpecialty(dto.specialty());
        a.setStatus(dto.status());
        return appointmentService.create(patientId, a)
                .map(AppointmentResponseDTO::fromEntity)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update an existing appointment")
    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> update(
            @PathVariable Long id, @RequestBody AppointmentRequestDTO dto) {
        Appointment a = new Appointment();
        a.setDateTime(dto.dateTime());
        a.setSpecialty(dto.specialty());
        a.setStatus(dto.status());
        return appointmentService.update(id, a)
                .map(AppointmentResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete an appointment")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!appointmentService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
