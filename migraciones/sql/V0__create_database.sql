-- Ejecutar en la VM de SQL Server antes del primer arranque del backend.
-- Flyway aplicará V1–V4 automáticamente al iniciar la aplicación.
IF DB_ID('inventario_egi') IS NULL
    CREATE DATABASE inventario_egi;
GO
