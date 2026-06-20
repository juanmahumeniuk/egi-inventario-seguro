package com.itu.egi.inventarioseguro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Servicio encargado de la gestión de tokens JWT (JSON Web Tokens).
 * Proporciona funcionalidad para generar nuevos tokens, validarlos y extraer información 
 * (como el nombre de usuario y roles) a partir de un token firmado.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * Construye un nuevo JwtService configurando la clave de firma HMAC a partir del 
     * secreto y estableciendo el tiempo de expiración del token.
     *
     * @param secret Secreto de firma cargado desde las propiedades de configuración.
     * @param expirationMs Tiempo de expiración del token en milisegundos.
     */
    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un token JWT firmado para un usuario específico y su conjunto de roles.
     *
     * @param username Nombre del usuario autenticado.
     * @param roles Conjunto de roles asignados al usuario.
     * @return El token JWT representado como una cadena compactada y firmada.
     */
    public String generateToken(String username, Set<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Valida si un token JWT es estructuralmente correcto, no ha expirado y está
     * correctamente firmado con la clave secreta de la aplicación.
     *
     * @param token El token JWT a validar.
     * @return true si el token es válido, false en caso contrario.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrae el nombre de usuario (subject) del cuerpo (claims) de un token JWT.
     *
     * @param token El token JWT.
     * @return El nombre de usuario contenido en el token.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Extrae la lista de roles del cuerpo (claims) de un token JWT y la devuelve como un conjunto.
     *
     * @param token El token JWT.
     * @return Conjunto de roles del usuario contenidos en el token; conjunto vacío si no se encuentran roles.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        List<String> roles = claims.get("roles", List.class);
        return roles != null ? Set.copyOf(roles) : Set.of();
    }
}
