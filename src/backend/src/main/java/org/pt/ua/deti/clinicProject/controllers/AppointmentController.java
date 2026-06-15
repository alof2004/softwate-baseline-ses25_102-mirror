package org.pt.ua.deti.clinicProject.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.pt.ua.deti.clinicProject.dto.AppointmentRequestDTO;
import org.pt.ua.deti.clinicProject.dto.AppointmentResponseDTO;
import org.pt.ua.deti.clinicProject.models.Appointment;
import org.pt.ua.deti.clinicProject.services.AppointmentService;
import org.pt.ua.deti.clinicProject.services.AuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private static final String RESOURCE = "appointments";

    private final AppointmentService appointmentService;
    private final AuditLogService auditLogService;

    public AppointmentController(
            AppointmentService appointmentService, AuditLogService auditLogService) {
        this.appointmentService = appointmentService;
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "List all appointments, optionally filtered")
    @GetMapping
    @PreAuthorize("@perms.canAnyRole(authentication, 'appointments', 'READ')")
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

    // Object-level (BOLA): service returns 404 if the appointment ID does not exist.
    @Operation(summary = "Get appointment by ID")
    @GetMapping("/{id}")
    @PreAuthorize("@perms.canAnyRole(authentication, 'appointments', 'READ')")
    public ResponseEntity<AppointmentResponseDTO> getById(@PathVariable UUID id) {
        return appointmentService.getById(id)
                .map(AppointmentResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new appointment for a patient")
    @PostMapping
    @PreAuthorize("@perms.canAnyRole(authentication, 'appointments', 'CREATE')")
    public ResponseEntity<AppointmentResponseDTO> create(
            @RequestParam UUID patientId, @Valid @RequestBody AppointmentRequestDTO dto) {
        Appointment a = new Appointment();
        a.setDateTime(dto.dateTime());
        a.setSpecialty(dto.specialty());
        a.setStatus(dto.status());
        a.setDoctorSub(dto.doctorSub());
        return appointmentService.create(patientId, a)
                .map(created -> {
                    auditLogService.log("CREATE", RESOURCE, String.valueOf(created.getId()));
                    return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentResponseDTO.fromEntity(created));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update an existing appointment")
    @PutMapping("/{id}")
    @PreAuthorize("@perms.canAnyRole(authentication, 'appointments', 'UPDATE')")
    public ResponseEntity<AppointmentResponseDTO> update(
            @PathVariable UUID id, @Valid @RequestBody AppointmentRequestDTO dto) {
        Appointment a = new Appointment();
        a.setDateTime(dto.dateTime());
        a.setSpecialty(dto.specialty());
        a.setStatus(dto.status());
        return appointmentService.update(id, a)
                .map(updated -> {
                    auditLogService.log("UPDATE", RESOURCE, String.valueOf(id));
                    return ResponseEntity.ok(AppointmentResponseDTO.fromEntity(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete an appointment")
    @DeleteMapping("/{id}")
    @PreAuthorize("@perms.canAnyRole(authentication, 'appointments', 'DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!appointmentService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        auditLogService.log("DELETE", RESOURCE, String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}
