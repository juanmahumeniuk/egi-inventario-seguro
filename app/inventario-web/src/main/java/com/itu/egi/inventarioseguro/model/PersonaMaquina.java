package com.itu.egi.inventarioseguro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "persona_maquina")
@Getter @Setter @NoArgsConstructor
public class PersonaMaquina {

    @EmbeddedId
    private PersonaMaquinaId id = new PersonaMaquinaId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("personaId")
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("maquinaId")
    @JoinColumn(name = "maquina_id")
    private Maquina maquina;

    @Column(name = "fecha_asignado")
    private LocalDate fechaAsignado;
}
