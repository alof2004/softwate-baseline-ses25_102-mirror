import keycloak from './keycloak.js'

// Mirrors the permission matrix in backend application.yml
const PERMISSIONS = {
  ADMIN:        { patients: ['READ','CREATE','UPDATE','DELETE'], appointments: ['READ','CREATE','UPDATE','DELETE'] },
  DOCTOR:       { patients: ['READ','UPDATE'],                   appointments: ['READ','UPDATE'] },
  RECEPTIONIST: { patients: ['READ','CREATE'],                   appointments: ['READ','CREATE','UPDATE','DELETE'] },
}

export function usePermissions() {
  const roles = keycloak.tokenParsed?.realm_access?.roles ?? []

  return {
    can(resource, action) {
      // eslint-disable-next-line security/detect-object-injection -- PERMISSIONS keys are role strings validated by Keycloak; resource is a known constant from caller
      return roles.some(role => PERMISSIONS[role]?.[resource]?.includes(action))
    },
  }
}
