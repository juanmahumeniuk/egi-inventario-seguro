package com.itu.egi.inventarioseguro.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Categoria {
    RESPONSABLE_TECNICO("Responsable Técnico"),
    ALUMNO("Alumno"),
    DOCENTE("Docente");

    private final String value;

    Categoria(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static Categoria fromValue(String value) {
        for (Categoria c : values()) {
            if (c.value.equalsIgnoreCase(value) || c.name().equalsIgnoreCase(value)) return c;
        }
        throw new IllegalArgumentException("Categoria desconocida: " + value);
    }
}
