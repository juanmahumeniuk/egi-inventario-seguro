package com.itu.egi.inventarioseguro.security;

import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Componente encargado de mapear los grupos obtenidos desde el servidor LDAP a roles internos de la aplicación.
 * Realiza la limpieza del Distinguished Name (DN) de los grupos para extraer únicamente el Common Name (CN)
 * y lo asocia con los roles de seguridad de Spring Security (ROLE_ADMIN, ROLE_EDITOR, ROLE_READONLY).
 */
@Component
public class RoleMapper {

    /**
     * Mapea una colección de nombres de grupos de LDAP (que pueden estar en formato DN completo)
     * a un conjunto de roles internos de la aplicación.
     *
     * @param groups Colección de grupos obtenidos de LDAP.
     * @return Conjunto de roles internos mapeados correspondientes (ej. ROLE_ADMIN, ROLE_EDITOR).
     */
    public Set<String> mapGroupsToRoles(Collection<String> groups) {
        if (groups == null) {
            return Set.of();
        }
        return groups.stream()
                .map(this::cleanGroupName)
                .map(this::mapGroupToRole)
                .filter(role -> role != null)
                .collect(Collectors.toSet());
    }

    /**
     * Limpia el nombre del grupo de LDAP. Si el grupo viene en formato DN completo (ej. "CN=Grupo,OU=..."),
     * extrae y devuelve únicamente el valor del Common Name (CN).
     *
     * @param group Nombre o DN completo del grupo en LDAP.
     * @return Nombre limpio del grupo, o una cadena vacía si es nulo.
     */
    private String cleanGroupName(String group) {
        if (group == null) {
            return "";
        }
        // En AD, un grupo suele venir como DN entero, e.g., "CN=Grupo_BD_Laboratorio_A,OU=Laboratorios,DC=itu,DC=local"
        if (group.toUpperCase().startsWith("CN=")) {
            int commaIndex = group.indexOf(",");
            if (commaIndex != -1) {
                return group.substring(3, commaIndex).trim();
            } else {
                return group.substring(3).trim();
            }
        }
        return group.trim();
    }

    /**
     * Realiza el mapeo directo entre el nombre limpio de un grupo LDAP y un rol específico de la aplicación.
     *
     * @param cleanedGroup Nombre limpio del grupo de LDAP.
     * @return Rol correspondiente (ej. ROLE_ADMIN, ROLE_EDITOR, ROLE_READONLY), o null si el grupo no coincide con ningún rol esperado.
     */
    private String mapGroupToRole(String cleanedGroup) {
        if ("Grupo_BD_Laboratorio_A".equalsIgnoreCase(cleanedGroup)) {
            return "ROLE_ADMIN";
        } else if ("Grupo_BD_Laboratorio_C".equalsIgnoreCase(cleanedGroup)) {
            return "ROLE_EDITOR";
        } else if ("Grupo_BD_Laboratorio_R".equalsIgnoreCase(cleanedGroup)) {
            return "ROLE_READONLY";
        }
        return null;
    }
}
