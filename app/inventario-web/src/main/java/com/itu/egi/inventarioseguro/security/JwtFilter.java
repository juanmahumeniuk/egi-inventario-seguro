package com.itu.egi.inventarioseguro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filtro de seguridad que se ejecuta una vez por cada solicitud entrante ({@link OncePerRequestFilter}).
 * Su propósito es interceptar las solicitudes HTTP, buscar el token JWT en el encabezado de
 * Autorización (Authorization Header) con el esquema "Bearer", validar el token y establecer la 
 * autenticación del usuario en el contexto de seguridad de Spring Security si el token es válido.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Construye un nuevo filtro JwtFilter con el servicio de tokens JWT inyectado.
     *
     * @param jwtService Servicio para la validación y extracción de información de tokens JWT.
     */
    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Método interno del filtro que realiza la lógica de interceptación.
     * Verifica la presencia de un token Bearer en el encabezado de autorización, lo valida,
     * extrae el nombre de usuario y los roles para construir un {@link CustomUserPrincipal},
     * y registra al usuario autenticado en el {@link SecurityContextHolder}.
     * Finalmente, pasa la solicitud y la respuesta al siguiente filtro en la cadena.
     *
     * @param request Solicitud HTTP entrante.
     * @param response Respuesta HTTP saliente.
     * @param filterChain Cadena de filtros de seguridad y servlets.
     * @throws ServletException Si ocurre un error interno durante el procesamiento del servlet.
     * @throws IOException Si ocurre un error de E/S.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.validateToken(token)) {
            String username = jwtService.getUsernameFromToken(token);
            Set<String> roles = jwtService.getRolesFromToken(token);

            CustomUserPrincipal principal = new CustomUserPrincipal(username, roles);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
