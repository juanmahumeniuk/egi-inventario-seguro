package com.itu.egi.inventarioseguro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DiscoRequest {
    @NotBlank private String tipo;
    @NotNull  private Integer capacidadGb;
}
