-- =============================================================================
-- Migración V3: Tabla PersonaMaquina (relación N:M) — SQL Server / T-SQL
-- Única relación entre Persona y Maquina. Una persona puede tener varias
-- máquinas asignadas y una máquina puede estar asignada a varias personas.
-- =============================================================================

CREATE TABLE persona_maquina (
    persona_id     BIGINT NOT NULL,
    maquina_id     BIGINT NOT NULL,
    fecha_asignado DATE   NULL,
    CONSTRAINT pk_persona_maquina PRIMARY KEY (persona_id, maquina_id),
    CONSTRAINT fk_pm_persona
        FOREIGN KEY (persona_id) REFERENCES persona (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_pm_maquina
        FOREIGN KEY (maquina_id) REFERENCES maquina (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE INDEX idx_pm_maquina ON persona_maquina (maquina_id);
