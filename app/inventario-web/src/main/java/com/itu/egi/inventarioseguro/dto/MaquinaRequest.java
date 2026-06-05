package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.Aula;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MaquinaRequest {

    @NotNull private Aula aula;
    @NotNull private Integer numeroMesa;
    private LocalDate fechaMantenimiento;

    @Valid @NotNull private HardwareRequest hardware;
}
