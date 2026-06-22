package com.itu.egi.inventarioseguro.controller;

import com.itu.egi.inventarioseguro.dto.MaquinaConAsignacionesDTO;
import com.itu.egi.inventarioseguro.dto.MaquinaRequest;
import com.itu.egi.inventarioseguro.service.MaquinaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maquinas")
@RequiredArgsConstructor
public class MaquinaController {

    private final MaquinaService maquinaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'READONLY')")
    public ResponseEntity<List<MaquinaConAsignacionesDTO>> listar() {
        return ResponseEntity.ok(maquinaService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'READONLY')")
    public ResponseEntity<MaquinaConAsignacionesDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(maquinaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<MaquinaConAsignacionesDTO> crear(@Valid @RequestBody MaquinaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maquinaService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<MaquinaConAsignacionesDTO> actualizar(@PathVariable Long id, @Valid @RequestBody MaquinaRequest req) {
        return ResponseEntity.ok(maquinaService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        maquinaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
