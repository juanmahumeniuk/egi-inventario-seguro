package com.itu.egi.inventarioseguro;

import org.junit.jupiter.api.Test;

/**
 * El contexto completo de Spring requiere Docker (SQL Server + MongoDB).
 * Levantarlo con: docker compose -f docker-compose.dev.yml up -d
 * Tests de integración real: MaquinaServiceTest y MaquinaControllerTest.
 */
class InventarioApplicationTests {

    @Test
    void placeholder() {
        // Los tests funcionales están en MaquinaServiceTest y MaquinaControllerTest.
        // Un test de contextLoads real requiere las bases de datos del docker-compose.
    }
}
