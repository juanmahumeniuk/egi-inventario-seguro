package com.itu.egi.inventarioseguro.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Punto de entrada de autenticación personalizado para la API protegida con JWT.
 * Implementa {@link AuthenticationEntryPoint} para capturar los errores de autenticación
 * (por ejemplo, tokens JWT faltantes, inválidos o expirados) y devolver una respuesta
 * JSON estructurada con el código de estado HTTP 401 (Unauthorized).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Se invoca cuando un usuario no autenticado intenta acceder a un recurso protegido.
     * Configura la respuesta HTTP con el tipo de contenido JSON, el estado 401 (Unauthorized)
     * y escribe un cuerpo JSON que detalla el error.
     *
     * @param request La solicitud HTTP entrante.
     * @param response La respuesta HTTP saliente.
     * @param authException La excepción de autenticación que originó el rechazo.
     * @throws IOException Si ocurre un error de E/S al escribir en la respuesta.
     * @throws ServletException Si ocurre una excepción interna de servlet.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getOutputStream().println("{ \"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\" }");
    }
}
