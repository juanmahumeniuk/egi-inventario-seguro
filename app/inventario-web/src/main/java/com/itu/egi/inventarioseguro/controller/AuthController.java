package com.itu.egi.inventarioseguro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> req) {
        String username = req.getOrDefault("username", "user");
        String role = "admin".equalsIgnoreCase(username) ? "ADMIN" : "TECNICO";
        return ResponseEntity.ok(Map.of(
                "username", username,
                "token", "mock-jwt-" + UUID.randomUUID(),
                "role", role
        ));
    }
}
