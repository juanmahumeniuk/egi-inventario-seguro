-- Alinea la tabla maquina con el diagrama de clases actualizado:
-- elimina la columna laboratorio (absorbida por el enum Aula)
-- y agrega el CHECK constraint para los valores válidos del enum.

ALTER TABLE maquina DROP COLUMN laboratorio;

ALTER TABLE maquina ADD CONSTRAINT ck_maquina_aula
    CHECK (aula IN ('AULA_1y2', 'LABORATORIO_SO', 'LABORATORIO_REDES', 'AULA_4'));
