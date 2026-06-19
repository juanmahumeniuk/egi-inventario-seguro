package com.itu.egi.inventarioseguro.security;

import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoleMapper {

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
