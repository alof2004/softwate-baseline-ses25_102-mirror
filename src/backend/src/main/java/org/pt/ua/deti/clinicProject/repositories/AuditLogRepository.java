package org.pt.ua.deti.clinicProject.repositories;

import org.pt.ua.deti.clinicProject.models.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
