package com.itu.egi.inventarioseguro.controller;

import com.itu.egi.inventarioseguro.dto.MaquinaConAsignacionesDTO;
import com.itu.egi.inventarioseguro.dto.MaquinaRequest;
import com.itu.egi.inventarioseguro.service.MaquinaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maquinas")
@RequiredArgsConstructor
public class MaquinaController {

    private final MaquinaService maquinaService;

    @GetMapping
    public ResponseEntity<List<MaquinaConAsignacionesDTO>> listar() {
        return ResponseEntity.ok(maquinaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaquinaConAsignacionesDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(maquinaService.findById(id));
    }

    @PostMapping
    public ResponseEntity<MaquinaConAsignacionesDTO> crear(@Valid @RequestBody MaquinaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maquinaService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaquinaConAsignacionesDTO> actualizar(@PathVariable Long id, @Valid @RequestBody MaquinaRequest req) {
        return ResponseEntity.ok(maquinaService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        maquinaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
