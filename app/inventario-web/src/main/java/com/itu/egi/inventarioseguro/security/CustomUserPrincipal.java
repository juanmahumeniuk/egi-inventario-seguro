package com.itu.egi.inventarioseguro.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación personalizada de {@link UserDetails} para representar los detalles
 * de seguridad del usuario autenticado en el contexto de Spring Security.
 * Almacena el nombre de usuario y su conjunto de roles mapeados como autoridades.
 */
public class CustomUserPrincipal implements UserDetails {

    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Construye un nuevo CustomUserPrincipal con el nombre de usuario y el conjunto de roles.
     * Los roles de texto se convierten internamente a objetos {@link SimpleGrantedAuthority}.
     *
     * @param username Nombre del usuario autenticado.
     * @param roles Conjunto de roles del usuario (ej. ROLE_ADMIN, ROLE_EDITOR).
     */
    public CustomUserPrincipal(String username, Set<String> roles) {
        this.username = username;
        this.authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    /**
     * Devuelve las autoridades otorgadas al usuario.
     *
     * @return Colección de autoridades otorgadas.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Devuelve la contraseña utilizada para autenticar al usuario.
     * Retorna null ya que la autenticación se maneja vía LDAP y no se almacena localmente.
     *
     * @return null.
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Devuelve el nombre de usuario utilizado para autenticar al usuario.
     *
     * @return El nombre de usuario.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Indica si la cuenta del usuario ha expirado.
     *
     * @return true si la cuenta del usuario es válida (no ha expirado), false de lo contrario.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica si el usuario está bloqueado o desbloqueado.
     *
     * @return true si el usuario no está bloqueado, false de lo contrario.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indica si las credenciales (contraseña) del usuario han expirado.
     *
     * @return true si las credenciales son válidas (no han expirado), false de lo contrario.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica si el usuario está habilitado o deshabilitado.
     *
     * @return true si el usuario está habilitado, false de lo contrario.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
