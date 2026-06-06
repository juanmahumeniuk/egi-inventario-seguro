# Resumen de Adaptación del Frontend e Integración de Servicios

Hemos adaptado el frontend de React + TypeScript para comunicarse de forma asíncrona con el backend mediante una arquitectura de servicios estructurada con soporte para JWT, utilizando la API nativa de `fetch`. Además, implementamos un mecanismo de simulación asíncrona (Mock) controlado por variables de entorno para que el frontend sea 100% interactivo y visualizable antes de que el backend sea desarrollado.

---

## Cambios Realizados

1. **Variables de Entorno:**
   - Creado archivo [.env](file:///c:/Luciano/Programming%20Projects/egi-inventario-seguro/app/inventario-web/frontend/.env) para cambiar fácilmente entre mocks (`VITE_USE_MOCK=true`) y el backend real de Spring Boot (`VITE_API_URL`).
   - Creado [vite-env.d.ts](file:///c:/Luciano/Programming%20Projects/egi-inventario-seguro/app/inventario-web/frontend/src/vite-env.d.ts) para habilitar el autocompletado y tipado de Vite en TypeScript.
2. **Capa de Servicios de API:**
   - [api.ts](file:///c:/Luciano/Programming%20Projects/egi-inventario-seguro/app/inventario-web/frontend/src/services/api.ts): Cliente base que inyecta automáticamente la cabecera `Authorization: Bearer <token>` si hay un JWT guardado. Contiene el simulador de transacciones base.
   - [authService.ts](file:///c:/Luciano/Programming%20Projects/egi-inventario-seguro/app/inventario-web/frontend/src/services/authService.ts): Maneja la autenticación LDAP/JWT asíncrona.
   - [maquinaService.ts](file:///c:/Luciano/Programming%20Projects/egi-inventario-seguro/app/inventario-web/frontend/src/services/maquinaService.ts): Abstrae los métodos CRUD asíncronos (`getMaquinas`, `getMaquinaById`, `createMaquina`, `updateMaquina`, `deleteMaquina`).
3. **Optimización de Interfaz y Estados en React:**
   - [LoginScreen.tsx](file:///c:/Luciano/Programming%20Projects/egi-inventario-seguro/app/inventario-web/frontend/src/components/LoginScreen.tsx): Adaptada para consumir `authService.login` asíncronamente con su spinner de carga y control de errores del backend LDAP.
   - [App.tsx](file:///c:/Luciano/Programming%20Projects/egi-inventario-seguro/app/inventario-web/frontend/src/App.tsx): Refactorizado para usar hooks asíncronos y mostrar un skeleton loader (*loading skeletons*) de carga en la tabla principal mientras los datos son obtenidos del servidor.

---

## Diseño de API y Endpoints Tentativos (Guía para Integrante 4 - Spring Boot)

El frontend está listo para comunicarse con la siguiente estructura de controladores REST en Spring Boot (`http://localhost:8080/api`):

### 1. Autenticación LDAP
- **Endpoint:** `POST /api/auth/login`
- **Request Body:**
  ```json
  {
    "username": "usuarioLdap",
    "password": "miPassword"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "username": "usuarioLdap",
    "token": "JWT_TOKEN_GENERADO_POR_SPRING_BOOT",
    "role": "ADMIN"
  }
  ```

### 2. Obtener Lista de Equipos (Consulta Combinada SQL + MongoDB)
- **Endpoint:** `GET /api/maquinas`
- **Flujo interno en backend:**
  1. Consultar a SQL Server (`ubicacion-db`) para obtener todas las filas de la tabla `Maquina` y sus asignaciones correspondientes mediante joins con `PersonaMaquina` y `Persona`.
  2. Para cada máquina obtenida, realizar una consulta a MongoDB (`inventario-db`) buscando el documento de la colección `maquinas` cuyo `maquina_id` coincida con el ID de la máquina SQL.
  3. Unificar los datos de ubicación (SQL) y especificaciones (MongoDB) y retornar la lista en formato JSON.
- **Response (200 OK):**
  ```json
  [
    {
      "id": 1,
      "numero_mesa": 5,
      "fecha_mantenimiento": "2026-06-01",
      "aula": "AULA_1y2",
      "especificaciones": {
        "fabricante": "Dell",
        "modelo": "OptiPlex 7090",
        "tipo": "desktop",
        "cpu": "Intel Core i5-11500",
        "ram_gb": 16,
        "disco": { "tipo": "SSD", "capacidad_gb": 512 },
        "sistema_operativo": "Windows 11 Pro",
        "perifericos": {
          "monitor": "Dell P2422H 24\"",
          "mouse": "Dell MS116",
          "teclado": "Dell KB216"
        }
      },
      "asignaciones": [
        {
          "persona": {
            "id": 101,
            "nombre": "Gonzalo",
            "apellido": "Morales",
            "categoria": "Alumno"
          },
          "fecha_asignado": "2026-06-01"
        }
      ]
    }
  ]
  ```

### 3. Crear una Nueva Máquina
- **Endpoint:** `POST /api/maquinas`
- **Request Body:**
  ```json
  {
    "numero_mesa": 5,
    "aula": "AULA_1y2",
    "fecha_mantenimiento": "2026-06-01",
    "especificaciones": {
      "fabricante": "Dell",
      "modelo": "OptiPlex 7090",
      "tipo": "desktop",
      "cpu": "Intel Core i5-11500",
      "ram_gb": 16,
      "disco": { "tipo": "SSD", "capacidad_gb": 512 },
      "sistema_operativo": "Windows 11 Pro",
      "perifericos": { "monitor": "Dell P2422H 24\"", "mouse": "Dell MS116", "teclado": "Dell KB216" }
    },
    "asignaciones": [
      { "personaId": 101, "fecha_asignado": "2026-06-01" }
    ]
  }
  ```
- **Flujo interno en backend:**
  1. Insertar en SQL Server en la tabla `Maquina` (retornar la primary key autoincremental `id`).
  2. Insertar en SQL Server en la tabla intermedia `PersonaMaquina` las asignaciones de personas.
  3. Insertar un documento en la colección de MongoDB con el campo `maquina_id` igual al ID recién generado.
- **Response (201 Created):**
  ```json
  { "id": 43 }
  ```

---

## Verificación

Se verificó el correcto funcionamiento ejecutando:
- `npx tsc --noEmit` confirmando que TypeScript compila perfectamente sin warnings.
- Comprobación visual asíncrona: El sistema responde correctamente a las interacciones CRUD mediante mock asíncrono con delays realistas, visualizándose el skeleton loading en la tabla durante las recargas.
