-- =============================================================================
-- Migración V2: Tabla Maquina (SQL Server / T-SQL)
-- El id es compartido con el documento equivalente en MongoDB.
-- "A quién le pertenece" se modela como FK directa al dueño (relación 1:N).
-- =============================================================================

CREATE TABLE maquina (
    id                  BIGINT       NOT NULL IDENTITY(1, 1),
    aula                NVARCHAR(50) NOT NULL,
    laboratorio         NVARCHAR(50) NOT NULL,
    numero_mesa         INT          NOT NULL,
    fecha_mantenimiento DATE         NULL,
    persona_id          BIGINT       NULL,
    CONSTRAINT pk_maquina PRIMARY KEY (id),
    CONSTRAINT fk_maquina_persona
        FOREIGN KEY (persona_id) REFERENCES persona (id)
        ON UPDATE CASCADE
        ON DELETE SET NULL
);

CREATE INDEX idx_maquina_persona ON maquina (persona_id);
