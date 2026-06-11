package org.pt.ua.deti.clinicProject.services;

import java.time.LocalDateTime;
import java.util.List;
import org.pt.ua.deti.clinicProject.models.AuditLog;
import org.pt.ua.deti.clinicProject.repositories.AuditLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String action, String resource, String resourceId) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setResource(resource);
        entry.setResourceId(resourceId);
        entry.setTimestamp(LocalDateTime.now());
        repository.save(entry);
    }

    public List<AuditLog> getAll() {
        return repository.findAll();
    }
}
