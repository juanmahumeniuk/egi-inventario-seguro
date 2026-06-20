package com.itu.egi.inventarioseguro.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio encargado de la autenticación de usuarios y obtención de grupos contra un servidor LDAP/Active Directory.
 * Utiliza {@link LdapTemplate} de Spring LDAP para realizar la validación de credenciales del usuario
 * y posteriormente recuperar los grupos a los que pertenece, tanto por pertenencia directa (atributo memberOf)
 * como a través de consultas reversas (filtro member con el Distinguished Name completo del usuario).
 */
@Service
public class LdapAuthenticationService {

    private final LdapTemplate ldapTemplate;

    @Value("${app.ldap.user-search-base:ou=Users}")
    private String userSearchBase;

    @Value("${app.ldap.user-search-filter:(sAMAccountName={0})}")
    private String userSearchFilter;

    @Value("${app.ldap.group-search-base:ou=Groups,ou=Laboratorios,ou=ITU}")
    private String groupSearchBase;

    @Value("${app.ldap.group-search-filter:(member={0})}")
    private String groupSearchFilter;

    /**
     * Construye un nuevo LdapAuthenticationService con el LdapTemplate inyectado.
     *
     * @param ldapTemplate Plantilla de Spring LDAP para realizar operaciones en el servidor de directorio.
     */
    public LdapAuthenticationService(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    /**
     * Autentica a un usuario con sus credenciales (nombre de usuario y contraseña) en el LDAP y 
     * obtiene el conjunto de grupos a los que pertenece.
     * <p>
     * El proceso de obtención de grupos consta de dos métodos complementarios:
     * 1. Extracción del atributo {@code memberOf} directamente de los atributos del usuario autenticado (común en Active Directory).
     * 2. Búsqueda de grupos en la base configurada usando el Distinguished Name (DN) del usuario con el filtro de miembros (común en OpenLDAP).
     * </p>
     *
     * @param username Nombre de usuario a autenticar (ej. sAMAccountName).
     * @param password Contraseña del usuario.
     * @return Conjunto de nombres distinguidos (DNs) o nombres de los grupos de LDAP a los que pertenece el usuario.
     * @throws RuntimeException Si las credenciales son incorrectas o falla la comunicación con el servidor LDAP.
     */
    public Set<String> authenticateAndGetGroups(String username, String password) {
        String formattedFilter = userSearchFilter.replace("{0}", username);
        
        try {
            ldapTemplate.authenticate(
                    LdapQueryBuilder.query().base(userSearchBase).filter(formattedFilter),
                    password
            );
        } catch (Exception e) {
            throw new RuntimeException("Credenciales de LDAP incorrectas", e);
        }

        Set<String> allGroups = new HashSet<>();

        // 1. Intentar obtener grupos del usuario usando el atributo memberOf (común en AD)
        List<UserLdapInfo> userInfoList = ldapTemplate.search(
                LdapQueryBuilder.query().base(userSearchBase).filter(formattedFilter),
                new AttributesMapper<UserLdapInfo>() {
                    @Override
                    public UserLdapInfo mapFromAttributes(Attributes attributes) throws javax.naming.NamingException {
                        List<String> memberOf = new ArrayList<>();
                        var memberOfAttr = attributes.get("memberOf");
                        if (memberOfAttr != null) {
                            for (int i = 0; i < memberOfAttr.size(); i++) {
                                memberOf.add(memberOfAttr.get(i).toString());
                            }
                        }
                        return new UserLdapInfo(memberOf);
                    }
                }
        );

        if (userInfoList != null && !userInfoList.isEmpty()) {
            allGroups.addAll(userInfoList.get(0).memberOf());
        }

        // 2. Buscar grupos usando el DN completo del usuario en el filtro member (común en OpenLDAP/AD alternativo)
        try {
            List<String> userFullDns = ldapTemplate.search(
                    LdapQueryBuilder.query().base(userSearchBase).filter(formattedFilter),
                    (org.springframework.ldap.core.ContextMapper<String>) ctx -> {
                        org.springframework.ldap.core.DirContextOperations adapter = (org.springframework.ldap.core.DirContextOperations) ctx;
                        return adapter.getNameInNamespace();
                    }
            );

            if (userFullDns != null && !userFullDns.isEmpty()) {
                String fullUserDn = userFullDns.get(0);
                String formattedGroupFilter = groupSearchFilter.replace("{0}", fullUserDn);
                
                List<String> groupsByMember = ldapTemplate.search(
                        LdapQueryBuilder.query().base(groupSearchBase).filter(formattedGroupFilter),
                        (org.springframework.ldap.core.ContextMapper<String>) ctx -> {
                            org.springframework.ldap.core.DirContextOperations adapter = (org.springframework.ldap.core.DirContextOperations) ctx;
                            return adapter.getNameInNamespace();
                        }
                );
                
                if (groupsByMember != null) {
                    allGroups.addAll(groupsByMember);
                }
            }
        } catch (Exception e) {
            // Ignorar errores menores de consulta de grupos directa
            System.err.println("Error buscando grupos por pertenencia directa: " + e.getMessage());
        }

        return allGroups;
    }

    private record UserLdapInfo(List<String> memberOf) {}
}
