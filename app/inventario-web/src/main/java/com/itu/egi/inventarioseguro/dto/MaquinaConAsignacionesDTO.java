package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.Aula;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MaquinaConAsignacionesDTO {
    private Long id;
    private Integer numeroMesa;
    private LocalDate fechaMantenimiento;
    private Aula aula;
    private EspecificacionesDTO especificaciones;
    private List<AsignacionDTO> asignaciones;
}
