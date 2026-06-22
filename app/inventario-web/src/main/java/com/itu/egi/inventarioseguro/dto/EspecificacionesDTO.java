package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.TipoEquipo;
import lombok.Data;

@Data
public class EspecificacionesDTO {
    private Long maquinaId;
    private String fabricante;
    private String modelo;
    private TipoEquipo tipo;
    private String cpu;
    private Integer ramGb;
    private DiscoDTO disco;
    private String sistemaOperativo;
    private PerifericosDTO perifericos;
}
