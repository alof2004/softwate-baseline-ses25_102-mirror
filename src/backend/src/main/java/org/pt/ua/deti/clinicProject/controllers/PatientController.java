package org.pt.ua.deti.clinicProject.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.pt.ua.deti.clinicProject.dto.PatientRequestDTO;
import org.pt.ua.deti.clinicProject.dto.PatientResponseDTO;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.pt.ua.deti.clinicProject.services.AuditLogService;
import org.pt.ua.deti.clinicProject.services.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Patients", description = "Manage clinic patients")
@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patientService;
    private final AuditLogService auditLogService;

    public PatientController(PatientService patientService, AuditLogService auditLogService) {
        this.patientService = patientService;
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "List all patients")
    @GetMapping
    @PreAuthorize("@perms.canAnyRole(authentication, 'patients', 'READ')")
    public List<PatientResponseDTO> getAll() {
        return patientService.getAll().stream().map(PatientResponseDTO::fromEntity).toList();
    }

    // Object-level (BOLA): service returns 404 if resource does not exist,
    // preventing enumeration of IDs the caller cannot access.
    @Operation(summary = "Get patient by ID")
    @GetMapping("/{id}")
    @PreAuthorize("@perms.canAnyRole(authentication, 'patients', 'READ')")
    public ResponseEntity<PatientResponseDTO> getById(@PathVariable UUID id) {
        return patientService.getById(id)
                .map(PatientResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new patient")
    @PostMapping
    @PreAuthorize("@perms.canAnyRole(authentication, 'patients', 'CREATE')")
    public ResponseEntity<PatientResponseDTO> create(@Valid @RequestBody PatientRequestDTO dto) {
        Patient p = new Patient();
        p.setName(dto.name());
        p.setDateOfBirth(dto.dateOfBirth());
        p.setPhoneNumber(dto.phoneNumber());
        p.setEmail(dto.email());
        Patient created = patientService.create(p);
        auditLogService.log("CREATE", "patients", String.valueOf(created.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientResponseDTO.fromEntity(created));
    }

    @Operation(summary = "Update an existing patient")
    @PutMapping("/{id}")
    @PreAuthorize("@perms.canAnyRole(authentication, 'patients', 'UPDATE')")
    public ResponseEntity<PatientResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody PatientRequestDTO dto) {
        Patient p = new Patient();
        p.setName(dto.name());
        p.setDateOfBirth(dto.dateOfBirth());
        p.setPhoneNumber(dto.phoneNumber());
        p.setEmail(dto.email());
        return patientService.update(id, p)
                .map(updated -> {
                    auditLogService.log("UPDATE", "patients", String.valueOf(id));
                    return ResponseEntity.ok(PatientResponseDTO.fromEntity(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a patient")
    @DeleteMapping("/{id}")
    @PreAuthorize("@perms.canAnyRole(authentication, 'patients', 'DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!patientService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        auditLogService.log("DELETE", "patients", String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}
