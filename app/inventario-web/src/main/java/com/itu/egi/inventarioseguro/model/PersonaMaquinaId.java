package com.itu.egi.inventarioseguro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Data @NoArgsConstructor @AllArgsConstructor
public class PersonaMaquinaId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "persona_id")
    private Long personaId;

    @Column(name = "maquina_id")
    private Long maquinaId;
}
