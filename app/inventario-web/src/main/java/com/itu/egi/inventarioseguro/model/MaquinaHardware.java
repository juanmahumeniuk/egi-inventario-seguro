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
    private String cpu;
    private String ram;
    private String disco;
    private TipoEquipo tipo;
    private String os;

    private String monitor;
    private String mouse;
    private String teclado;
}
