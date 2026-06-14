package org.pt.ua.deti.clinicProject.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditLogProtectionRunner {

    private final DataSource dataSource;

    public AuditLogProtectionRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void revokeAuditLogMutations() throws Exception {
        String user = dataSource.getConnection().getMetaData().getUserName();
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("REVOKE UPDATE, DELETE ON TABLE audit_logs FROM \"" + user + "\"");
        } catch (Exception e) {
            // REVOKE may fail if already revoked or if the role is a superuser —
            // log and continue rather than preventing startup.
            System.err.println("[AuditLogProtection] REVOKE skipped: " + e.getMessage());
        }
    }
}
