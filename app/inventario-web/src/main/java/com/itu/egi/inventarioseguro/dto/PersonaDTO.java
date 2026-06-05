package com.itu.egi.inventarioseguro.dto;

import com.itu.egi.inventarioseguro.model.Categoria;
import lombok.Data;

@Data
public class PersonaDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private Categoria categoria;
}
