-- =============================================================================
-- Migración V2: Tabla Maquina (SQL Server / T-SQL)
-- El id es compartido con el documento equivalente en MongoDB.
-- La relación con Persona se resuelve exclusivamente mediante la tabla
-- intermedia persona_maquina (ver V3); no hay FK directa hacia Persona.
-- =============================================================================

CREATE TABLE maquina (
    id                  BIGINT       NOT NULL IDENTITY(1, 1),
    aula                NVARCHAR(50) NOT NULL,
    laboratorio         NVARCHAR(50) NOT NULL,
    numero_mesa         INT          NOT NULL,
    fecha_mantenimiento DATE         NULL,
    CONSTRAINT pk_maquina PRIMARY KEY (id)
);
