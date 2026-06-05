package com.itu.egi.inventarioseguro.controller;

import com.itu.egi.inventarioseguro.dto.PersonaDTO;
import com.itu.egi.inventarioseguro.dto.PersonaRequest;
import com.itu.egi.inventarioseguro.service.PersonaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    @GetMapping
    public ResponseEntity<List<PersonaDTO>> listar() {
        return ResponseEntity.ok(personaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonaDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(personaService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PersonaDTO> crear(@Valid @RequestBody PersonaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personaService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonaDTO> actualizar(@PathVariable Long id, @Valid @RequestBody PersonaRequest req) {
        return ResponseEntity.ok(personaService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        personaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
