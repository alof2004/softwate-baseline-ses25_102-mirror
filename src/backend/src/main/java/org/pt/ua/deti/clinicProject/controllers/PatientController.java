package org.pt.ua.deti.clinicProject.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.pt.ua.deti.clinicProject.dto.PatientRequestDTO;
import org.pt.ua.deti.clinicProject.dto.PatientResponseDTO;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.pt.ua.deti.clinicProject.services.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Patients", description = "Manage clinic patients")
@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Operation(summary = "List all patients")
    @GetMapping
    public List<PatientResponseDTO> getAll() {
        return patientService.getAll().stream().map(PatientResponseDTO::fromEntity).toList();
    }

    @Operation(summary = "Get patient by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> getById(@PathVariable Long id) {
        return patientService.getById(id)
                .map(PatientResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new patient")
    @PostMapping
    public ResponseEntity<PatientResponseDTO> create(@RequestBody PatientRequestDTO dto) {
        Patient p = new Patient();
        p.setName(dto.name());
        p.setDateOfBirth(dto.dateOfBirth());
        p.setPhoneNumber(dto.phoneNumber());
        p.setEmail(dto.email());
        Patient created = patientService.create(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientResponseDTO.fromEntity(created));
    }

    @Operation(summary = "Update an existing patient")
    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> update(@PathVariable Long id, @RequestBody PatientRequestDTO dto) {
        Patient p = new Patient();
        p.setName(dto.name());
        p.setDateOfBirth(dto.dateOfBirth());
        p.setPhoneNumber(dto.phoneNumber());
        p.setEmail(dto.email());
        return patientService.update(id, p)
                .map(PatientResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a patient")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!patientService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
