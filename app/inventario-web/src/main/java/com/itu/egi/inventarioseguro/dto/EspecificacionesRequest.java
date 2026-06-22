package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.TipoEquipo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EspecificacionesRequest {
    @NotBlank private String fabricante;
    @NotBlank private String modelo;
    @NotNull  private TipoEquipo tipo;
    @NotBlank private String cpu;
    @NotNull  private Integer ramGb;
    @Valid @NotNull private DiscoRequest disco;
    @NotBlank private String sistemaOperativo;
    private PerifericosRequest perifericos;
}
