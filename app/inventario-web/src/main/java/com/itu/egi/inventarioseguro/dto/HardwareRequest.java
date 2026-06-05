package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.TipoEquipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HardwareRequest {

    @NotBlank private String fabricante;
    @NotBlank private String modelo;
    @NotBlank private String cpu;
    @NotBlank private String ram;
    @NotBlank private String disco;
    @NotNull  private TipoEquipo tipo;
    @NotBlank private String os;

    private String monitor;
    private String mouse;
    private String teclado;
}
