package org.pt.ua.deti.clinicProject.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.pt.ua.deti.clinicProject.models.AuditLog;
import org.pt.ua.deti.clinicProject.services.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Logs", description = "Read-only access to the security audit trail (ADMIN only)")
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "List all audit log entries")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getAll() {
        return auditLogService.getAll();
    }
}
