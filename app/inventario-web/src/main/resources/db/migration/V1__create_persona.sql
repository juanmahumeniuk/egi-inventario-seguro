CREATE TABLE persona (
    id        BIGINT        NOT NULL IDENTITY(1, 1),
    nombre    NVARCHAR(100) NOT NULL,
    apellido  NVARCHAR(100) NOT NULL,
    categoria NVARCHAR(20)  NOT NULL,
    CONSTRAINT pk_persona PRIMARY KEY (id),
    CONSTRAINT ck_persona_categoria
        CHECK (categoria IN ('RESPONSABLE_TECNICO', 'ALUMNO', 'DOCENTE'))
);
