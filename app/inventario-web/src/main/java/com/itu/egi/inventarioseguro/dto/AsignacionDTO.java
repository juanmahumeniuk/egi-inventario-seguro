package com.itu.egi.inventarioseguro.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AsignacionDTO {
    private PersonaDTO persona;
    private LocalDate fechaAsignado;
}
