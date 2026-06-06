package com.itu.egi.inventarioseguro.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "maquina")
@Getter @Setter @NoArgsConstructor
public class MaquinaHardware {

    @Id
    private Long id;

    private String fabricante;
    private String modelo;
    private TipoEquipo tipo;
    private String cpu;
    private Integer ramGb;
    private Disco disco;
    private String sistemaOperativo;
    private Perifericos perifericos;
}
