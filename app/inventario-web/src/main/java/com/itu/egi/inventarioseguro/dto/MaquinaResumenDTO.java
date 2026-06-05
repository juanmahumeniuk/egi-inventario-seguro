package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.Aula;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MaquinaResumenDTO {
    private Long id;
    private Aula aula;
    private Integer numeroMesa;
    private LocalDate fechaMantenimiento;
}
