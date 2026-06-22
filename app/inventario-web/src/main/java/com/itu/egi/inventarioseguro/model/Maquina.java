package com.itu.egi.inventarioseguro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "maquina")
@Getter @Setter @NoArgsConstructor
public class Maquina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Aula aula;

    @Column(name = "numero_mesa", nullable = false)
    private Integer numeroMesa;

    @Column(name = "fecha_mantenimiento")
    private LocalDate fechaMantenimiento;

    @OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PersonaMaquina> asignaciones = new ArrayList<>();
}
