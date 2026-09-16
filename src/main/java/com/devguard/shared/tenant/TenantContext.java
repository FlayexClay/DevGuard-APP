package com.devguard.shared.tenant;

import java.util.Set;
import java.util.UUID;

/*
 * Identidad resuelta del soliccitante. Toda operacion de negocio debe pasar por qui para obtener el
 * organizationId; ninguna consulta debe aceptar un organizationId que venga del cuerpo o de la query
 * string de la peticion.
 */
public record TenantContext (
        UUID organizationId,
        String organizationSlug,
        String subject,
        String email,
        Set<String> roles) {

    public boolean hasRole(String role) { return roles.contains(role); }
}
