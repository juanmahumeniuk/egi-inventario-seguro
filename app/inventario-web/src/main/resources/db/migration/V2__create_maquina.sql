CREATE TABLE maquina (
    id                  BIGINT       NOT NULL IDENTITY(1, 1),
    aula                NVARCHAR(50) NOT NULL,
    laboratorio         NVARCHAR(50) NOT NULL,
    numero_mesa         INT          NOT NULL,
    fecha_mantenimiento DATE         NULL,
    CONSTRAINT pk_maquina PRIMARY KEY (id)
);
