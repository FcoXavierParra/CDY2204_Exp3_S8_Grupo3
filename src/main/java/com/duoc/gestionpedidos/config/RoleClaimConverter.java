package com.duoc.gestionpedidos.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Convierte el custom claim de rol del JWT de Azure AD B2C en autoridades de Spring Security.
 *
 * Ejemplo: claim extension_rol = "gestor"  ->  autoridad ROLE_GESTOR
 *          claim extension_rol = "consulta" ->  autoridad ROLE_CONSULTA
 *
 * Soporta que el valor venga como String simple o como lista de roles, y que el nombre
 * del claim venga con el prefijo de extension de B2C (extension_&lt;appId&gt;_rol).
 */
public class RoleClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String roleClaim;

    public RoleClaimConverter(String roleClaim) {
        this.roleClaim = roleClaim;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Object rawValue = resolveClaimValue(jwt);

        if (rawValue instanceof String valor) {
            addRole(authorities, valor);
        } else if (rawValue instanceof Collection<?> valores) {
            for (Object v : valores) {
                if (v != null) {
                    addRole(authorities, v.toString());
                }
            }
        }
        return authorities;
    }

    /** Busca el claim exacto y, si no existe, cualquier claim que termine en "rol"/"role". */
    private Object resolveClaimValue(Jwt jwt) {
        if (jwt.hasClaim(roleClaim)) {
            return jwt.getClaim(roleClaim);
        }
        for (String clave : jwt.getClaims().keySet()) {
            String lower = clave.toLowerCase();
            if (lower.startsWith("extension_") && (lower.endsWith("rol") || lower.endsWith("role"))) {
                return jwt.getClaim(clave);
            }
        }
        return null;
    }

    private void addRole(List<GrantedAuthority> authorities, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        String rol = valor.trim().toUpperCase();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
