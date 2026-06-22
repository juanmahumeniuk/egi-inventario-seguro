package com.itu.egi.inventarioseguro.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AsignacionRequest {

    @NotNull private Long personaId;
    @NotNull private Long maquinaId;
    private LocalDate fechaAsignado;
}
