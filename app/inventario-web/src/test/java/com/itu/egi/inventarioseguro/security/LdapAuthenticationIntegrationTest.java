package com.itu.egi.inventarioseguro.security;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para validar la conectividad en vivo con el Active
 * Directory / LDAP
 * y comprobar que la extracción y mapeo de grupos a roles funciona
 * correctamente.
 *
 * Para ejecutar este test:
 * 1. Asegúrate de tener conectividad con la red donde está el servidor LDAP
 * (ej. VPN, misma subnet virtual).
 * 2. Define las credenciales correctas en las variables 'username' y 'password'
 * dentro del test.
 * 3. Ejecuta: ./mvnw test -Dtest=LdapAuthenticationIntegrationTest
 */
@SpringBootTest(properties = {
    "spring.ldap.username=cn=Administrador,cn=Users,dc=itu,dc=local",
    "app.ldap.user-search-base=OU=Users,OU=ITU",
    "app.ldap.group-search-base=OU=Laboratorios,OU=Groups,OU=ITU"
})
public class LdapAuthenticationIntegrationTest {

    @Autowired
    private LdapAuthenticationService ldapAuthenticationService;

    @Autowired
    private RoleMapper roleMapper;

    @Test
    public void testRealLdapConnectionAndRoles() {
        // TODO: Reemplazar con un usuario y contraseña reales del Active Directory /
        // LDAP para probar
        String username = "lucianofedericci";
        String password = "Itu12345!";

        System.out.println("==========================================================");
        System.out.println("INICIANDO CONEXIÓN A LDAP: " + username);
        System.out.println("==========================================================");

        try {
            // 1. Validar Autenticación y Obtener Grupos
            Set<String> ldapGroups = ldapAuthenticationService.authenticateAndGetGroups(username, password);
            System.out.println("✅ Conexión y Autenticación con LDAP: EXITOSA");
            System.out.println("📁 Grupos obtenidos del usuario en Active Directory (memberOf):");
            if (ldapGroups.isEmpty()) {
                System.out.println("   [Ningún grupo encontrado para este usuario]");
            } else {
                ldapGroups.forEach(group -> System.out.println("   - " + group));
            }

            // 2. Validar Mapeo de Roles
            Set<String> internalRoles = roleMapper.mapGroupsToRoles(ldapGroups);
            System.out.println("🛡️  Roles internos de aplicación mapeados:");
            if (internalRoles.isEmpty()) {
                System.out.println("   ⚠️  ATENCIÓN: Ningún grupo coincide con los mapeados en RoleMapper.");
                System.out.println("      Grupos esperados por RoleMapper:");
                System.out.println("      - Grupo_BD_Laboratorio_A -> ROLE_ADMIN");
                System.out.println("      - Grupo_BD_Laboratorio_C -> ROLE_EDITOR");
                System.out.println("      - Grupo_BD_Laboratorio_R -> ROLE_READONLY");
            } else {
                internalRoles.forEach(role -> System.out.println("   - " + role));
            }

            assertNotNull(ldapGroups, "La lista de grupos no debería ser nula");
            System.out.println("==========================================================");

        } catch (Exception e) {
            System.err.println("❌ ERROR AL CONECTAR CON LDAP: " + e.getMessage());
            e.printStackTrace(); // Imprime el stack trace completo para ver el error original de AD
            System.err.println("Asegúrate de que el servidor LDAP en la IP/puerto configurado en application.yml está activo y accesible.");
            System.out.println("==========================================================");
            fail("Fallo la conexión o autenticación con el LDAP real: " + e.getMessage());
        }
    }
}
