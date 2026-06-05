package com.itu.egi.inventarioseguro.controller;

import com.itu.egi.inventarioseguro.dto.AsignacionRequest;
import com.itu.egi.inventarioseguro.service.AsignacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionService asignacionService;

    @PostMapping
    public ResponseEntity<Void> asignar(@Valid @RequestBody AsignacionRequest req) {
        asignacionService.asignar(req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{personaId}/{maquinaId}")
    public ResponseEntity<Void> desasignar(@PathVariable Long personaId, @PathVariable Long maquinaId) {
        asignacionService.desasignar(personaId, maquinaId);
        return ResponseEntity.noContent().build();
    }
}
