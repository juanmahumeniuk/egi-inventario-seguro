package com.itu.egi.inventarioseguro.controller;

import com.itu.egi.inventarioseguro.security.JwtService;
import com.itu.egi.inventarioseguro.security.LdapAuthenticationService;
import com.itu.egi.inventarioseguro.security.RoleMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LdapAuthenticationService ldapAuthenticationService;
    private final RoleMapper roleMapper;
    private final JwtService jwtService;

    public AuthController(LdapAuthenticationService ldapAuthenticationService,
                          RoleMapper roleMapper,
                          JwtService jwtService) {
        this.ldapAuthenticationService = ldapAuthenticationService;
        this.roleMapper = roleMapper;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        try {
            Set<String> ldapGroups = ldapAuthenticationService.authenticateAndGetGroups(username, password);
            Set<String> internalRoles = roleMapper.mapGroupsToRoles(ldapGroups);

            String token = jwtService.generateToken(username, internalRoles);

            // Determinar rol para compatibilidad con el frontend
            String compatibleRole = "READONLY";
            if (internalRoles.contains("ROLE_ADMIN")) {
                compatibleRole = "ADMIN";
            } else if (internalRoles.contains("ROLE_EDITOR")) {
                compatibleRole = "EDITOR";
            }

            return ResponseEntity.ok(Map.of(
                    "username", username,
                    "token", token,
                    "role", compatibleRole
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed", "message", e.getMessage()));
        }
    }
}
