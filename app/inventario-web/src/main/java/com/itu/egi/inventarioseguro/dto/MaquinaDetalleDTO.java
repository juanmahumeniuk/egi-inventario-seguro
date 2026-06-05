package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.Aula;
import com.itu.egi.inventarioseguro.model.TipoEquipo;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MaquinaDetalleDTO {
    private Long id;
    private Aula aula;
    private Integer numeroMesa;
    private LocalDate fechaMantenimiento;

    private String fabricante;
    private String modelo;
    private String cpu;
    private String ram;
    private String disco;
    private TipoEquipo tipo;
    private String os;
    private String monitor;
    private String mouse;
    private String teclado;

    private List<PersonaDTO> personas;
}
