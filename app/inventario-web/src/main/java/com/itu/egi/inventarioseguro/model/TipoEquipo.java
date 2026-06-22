package com.itu.egi.inventarioseguro.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoEquipo {
    DESKTOP, LAPTOP, ALL_IN_ONE;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace("_", "-");
    }

    @JsonCreator
    public static TipoEquipo fromValue(String value) {
        for (TipoEquipo t : values()) {
            if (t.getValue().equals(value)) return t;
        }
        throw new IllegalArgumentException("TipoEquipo desconocido: " + value);
    }
}
