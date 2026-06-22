-- =============================================================================
-- Migración V4: Alinea maquina con el enum Aula del diagrama de clases
-- Elimina la columna laboratorio (su información queda codificada en el enum)
-- y agrega el CHECK constraint sobre la columna aula.
-- =============================================================================

ALTER TABLE maquina DROP COLUMN laboratorio;

ALTER TABLE maquina ADD CONSTRAINT ck_maquina_aula
    CHECK (aula IN ('AULA_1y2', 'LABORATORIO_SO', 'LABORATORIO_REDES', 'AULA_4'));
