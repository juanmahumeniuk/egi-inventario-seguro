package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.Aula;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class MaquinaRequest {

    @NotNull private Aula aula;
    @NotNull private Integer numeroMesa;
    private LocalDate fechaMantenimiento;

    @Valid @NotNull private EspecificacionesRequest especificaciones;

    @Valid
    private List<AsignacionEnMaquinaRequest> asignaciones = new ArrayList<>();
}
