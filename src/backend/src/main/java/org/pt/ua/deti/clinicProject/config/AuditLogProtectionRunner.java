package org.pt.ua.deti.clinicProject.config;

import jakarta.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditLogProtectionRunner {

    private static final Logger log = Logger.getLogger(AuditLogProtectionRunner.class.getName());

    private final DataSource dataSource;

    public AuditLogProtectionRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void revokeAuditLogMutations() throws SQLException {
        String user;
        try (var metaConn = dataSource.getConnection()) {
            user = metaConn.getMetaData().getUserName();
        }
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("REVOKE UPDATE, DELETE ON TABLE audit_logs FROM \"" + user + "\"");
        } catch (SQLException e) {
            // REVOKE may fail if already revoked or if the role is a superuser —
            // log and continue rather than preventing startup.
            log.warning("[AuditLogProtection] REVOKE skipped: " + e.getMessage());
        }
    }
}
