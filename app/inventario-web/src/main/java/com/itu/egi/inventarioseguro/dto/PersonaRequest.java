package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PersonaRequest {

    @NotBlank private String nombre;
    @NotBlank private String apellido;
    @NotNull  private Categoria categoria;
}
