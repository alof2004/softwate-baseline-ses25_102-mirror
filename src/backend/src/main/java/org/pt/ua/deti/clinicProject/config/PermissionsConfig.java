package org.pt.ua.deti.clinicProject.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

// Roles and permissions expressed as data — loaded from application.yml (clinic.permissions)
@Component("perms")
@ConfigurationProperties(prefix = "clinic")
public class PermissionsConfig {

    private Map<String, Map<String, List<String>>> permissions = Collections.emptyMap();

    public Map<String, Map<String, List<String>>> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Map<String, List<String>>> permissions) {
        this.permissions = permissions;
    }

    // Used in @PreAuthorize("@perms.canAnyRole(authentication, 'patients', 'DELETE')")
    public boolean canAnyRole(Authentication auth, String resource, String action) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .anyMatch(role -> {
                    Map<String, List<String>> rolePerms = permissions.get(role);
                    if (rolePerms == null) return false;
                    List<String> actions = rolePerms.get(resource);
                    return actions != null && actions.contains(action);
                });
    }
}
