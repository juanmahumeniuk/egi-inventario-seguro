package com.itu.egi.inventarioseguro.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AsignacionEnMaquinaRequest {
    // El frontend manda "personaId" (camelCase), pero con SNAKE_CASE global
    // Jackson espera "persona_id". @JsonAlias acepta ambos.
    @JsonAlias("personaId")
    @NotNull private Long personaId;
    private LocalDate fechaAsignado;
}
