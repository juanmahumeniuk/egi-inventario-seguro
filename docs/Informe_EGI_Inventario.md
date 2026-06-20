## 1. Introducción

El presente informe documenta el diseño, análisis y planificación del sistema denominado **Ecosistema de Inventario Seguro (EGI)**, desarrollado como proyecto integrador para la asignatura correspondiente del Instituto Tecnológico Universitario (ITU).

El problema central que motiva este proyecto es la necesidad de la universidad de contar con un sistema centralizado y seguro para inventariar las computadoras de sus laboratorios de informática. Actualmente, la gestión del inventario de hardware y la trazabilidad de ubicación y responsabilidad de cada equipo se realizan de forma manual o descentralizada, lo que implica riesgos de pérdida de información, dificultad para auditorías y falta de visibilidad operativa.

La solución propuesta contempla el desarrollo de una **aplicación web** que integre dos bases de datos heterogéneas (SQL Server y MongoDB), un servidor de identidad centralizado (Active Directory/LDAP), y un despliegue contenerizado sobre Kubernetes (Minikube), cumpliendo con estrictas políticas de seguridad perimetral simuladas mediante GUFW/pfSense.

---

## 2. Objetivos

### 2.1 Objetivo General

Desarrollar un ecosistema de software seguro, contenerizado y versionado que permita inventariar las computadoras de los laboratorios del ITU, cumpliendo con los principios de mínimo privilegio, autenticación centralizada y arquitectura Zero-Trust.

### 2.2 Objetivos Específicos

- Diseñar e implementar una aplicación web capaz de consultar SQL Server para obtener datos de ubicación y asignación de equipos, y MongoDB para obtener los datos de hardware.
- Configurar un servidor Active Directory/LDAP para la autenticación centralizada de usuarios.
- Desplegar todos los componentes del sistema de forma contenerizada mediante Docker y orquestados con Kubernetes (Minikube con CNI Calico).
- Implementar NetworkPolicies de Kubernetes que garanticen el principio de mínimo privilegio en el tráfico de red interno del clúster.
- Simular un perímetro de seguridad mediante GUFW/pfSense que emule el entorno de firewall universitario.
- Versionar el proyecto completo en un repositorio Git con commits atómicos y organizados por responsabilidad.

---

## 3. Descripción del Sistema

### 3.1 Arquitectura General

El ecosistema está compuesto por cinco servicios principales, todos desplegados dentro de un clúster Kubernetes (Minikube) en un namespace aislado denominado `inventario-seguro`:

| Servicio | Tecnología | Puerto | Función |
|---|---|---|---|
| `inventario-web` | Spring Boot (Frontend + API REST) | 8080 (HTTP) | Interfaz gráfica y capa de negocio |
| `ubicacion-db` | SQL Server | 1433 (JDBC) | Datos de ubicación y asignación |
| `inventario-db` | MongoDB | 27017 (Mongo Wire) | Datos de hardware de cada equipo |
| `ldap-service` | OpenLDAP / Active Directory | 389 (LDAP) / 636 (LDAPS) | Autenticación institucional |
| Firewall / DMZ | GUFW / pfSense | — | Perímetro de seguridad perimetral |

El tráfico externo proveniente del usuario institucional ingresa vía HTTPS al perímetro (Firewall/DMZ), que filtra y reenvía las solicitudes legítimas al **Ingress Controller** del clúster Kubernetes. Desde allí, el tráfico se dirige exclusivamente al frontend (`inventario-web`), que es el único servicio con acceso a la capa de datos.

### 3.2 Diagrama de Topología del Sistema

```mermaid
graph TD
    U([Usuario Institucional])
    FW["Perímetro Zero-Trust\nFirewall / DMZ\n(GUFW · pfSense)"]
    IC["Ingress Controller\n(Kubernetes)"]

    subgraph NS["Namespace: inventario-seguro"]
        FE["inventario-web\nSpring Boot · Frontend + API"]
        
        subgraph DATA["Capa de Datos"]
            SQL["ubicacion-db\nSQL Server\n:1433"]
            MONGO["inventario-db\nMongoDB\n:27017"]
        end
        
        LDAP["ldap-service\nActive Directory / LDAP\n:389 / :636"]
    end

    subgraph PV["Persistencia (PersistentVolumes)"]
        PV1[PVC sqlserver-data]
        PV2[PVC mongodb-data]
        PV3[PVC ldap-data]
    end

    U -- "HTTPS" --> FW
    FW -- "HTTP" --> IC
    IC --> FE

    FE -- "JDBC :1433" --> SQL
    FE -- "Mongo Wire :27017" --> MONGO
    FE -- "LDAP/LDAPS :389/636" --> LDAP

    SQL -.-> PV1
    MONGO -.-> PV2
    LDAP -.-> PV3
```

---

## 4. Modelización de Bases de Datos

### 4.1 Base de Datos SQL Server — `ubicacion-db`

Esta base de datos almacena la información de **ubicación física** de cada máquina y su **asignación a personas** (docentes, alumnos, técnicos responsables).

#### 4.1.1 Diagrama de Clases (Modelo Relacional)

```mermaid
classDiagram
    class Persona {
        +BIGINT id PK «IDENTITY»
        +NVARCHAR(100) nombre
        +NVARCHAR(100) apellido
        +NVARCHAR(20) categoria «CHECK»
    }

    class Maquina {
        +BIGINT id PK «IDENTITY»
        +INT numero_mesa
        +DATE fecha_mantenimiento
        +NVARCHAR(50) aula «CHECK enum»
    }

    class PersonaMaquina {
        +BIGINT persona_id PK FK
        +BIGINT maquina_id PK FK
        +DATE fecha_asignado
    }

    Persona "1" --> "0..*" PersonaMaquina : asignacion
    Maquina "1" --> "0..*" PersonaMaquina : asignacion
```

#### 4.1.2 Descripción de Entidades

**Persona**
Representa a cualquier usuario institucional que puede tener equipos asignados: docentes, alumnos o responsables técnicos.
- `id`: Identificador único autoincremental.
- `nombre` / `apellido`: Datos identificatorios del individuo.
- `categoria`: Tipo de persona. Restringido mediante constraint `CHECK` a valores como `'docente'`, `'alumno'` o `'tecnico'`.

**Maquina**
Representa cada computadora física inventariada en los laboratorios.
- `id`: Identificador único autoincremental. Este mismo ID es la clave compartida con MongoDB.
- `numero_mesa`: Número del banco o mesa dentro del aula/laboratorio.
- `fecha_mantenimiento`: Fecha del último mantenimiento registrado.
- `aula`: Ubicación física del equipo. Restringida mediante constraint `CHECK` al enum `Aula`: `AULA_1y2`, `LABORATORIO_SO`, `LABORATORIO_REDES`, `AULA_4`.

**PersonaMaquina** *(Tabla de relación)*
Tabla de asociación que implementa la relación N:M entre `Persona` y `Maquina`, registrando las asignaciones temporales.
- `persona_id` y `maquina_id`: Clave primaria compuesta (también foráneas).
- `fecha_asignado`: Fecha en que se realizó la asignación.

### 4.2 Base de Datos MongoDB — `inventario-db`

MongoDB almacena los datos de **hardware interno** de cada equipo. Se utiliza una única colección denominada `maquinas`.

#### 4.2.1 Estructura del Documento (Schema)

El documento almacena los datos de hardware con objetos anidados para `disco` y `perifericos`, reemplazando los campos planos de String de la versión anterior.

```mermaid
graph LR
    DOC["Documento MongoDB\nColección: maquina"]
    DOC --> _id["_id: Long (= Maquina.id en SQL)"]
    DOC --> fabricante["fabricante: String"]
    DOC --> modelo["modelo: String"]
    DOC --> tipo["tipo: String\n(DESKTOP | LAPTOP | ALL_IN_ONE)"]
    DOC --> cpu["cpu: String"]
    DOC --> ramGb["ramGb: Integer (GB)"]
    DOC --> sistemaOperativo["sistemaOperativo: String"]
    DOC --> disco["disco: Disco"]
    DOC --> perifericos["perifericos: Perifericos (opcional)"]

    disco --> dt["tipo: String (SSD | HDD)"]
    disco --> dc["capacidadGb: Integer (GB)"]

    perifericos --> pm["monitor: String"]
    perifericos --> pmo["mouse: String"]
    perifericos --> pt["teclado: String"]
```

#### 4.2.2 Ejemplo de Documento JSON

```json
{
  "_id": 10001,
  "fabricante": "Dell",
  "modelo": "OptiPlex 7090",
  "tipo": "DESKTOP",
  "cpu": "Intel Core i5-11500",
  "ramGb": 16,
  "sistemaOperativo": "Windows 11 Pro",
  "disco": {
    "tipo": "SSD",
    "capacidadGb": 512
  },
  "perifericos": {
    "monitor": "Dell P2422H 24\"",
    "mouse": "Dell MS116",
    "teclado": "Dell KB216"
  },
  "_class": "com.itu.egi.inventarioseguro.model.MaquinaHardware"
}
```

El campo `_id` del documento MongoDB es el mismo `Long` que el `id` de la tabla `Maquina` en SQL Server. Esto elimina la necesidad de un campo de referencia separado y permite que la aplicación recupere ambas fuentes de datos con una sola clave. Los campos `disco` y `perifericos` son objetos embebidos (no colecciones separadas). El campo `_class` es agregado automáticamente por Spring Data MongoDB.

Notar que el documento almacenado usa los nombres de campo de las clases Java (camelCase: `ramGb`, `sistemaOperativo`) y los valores del enum en mayúsculas (`DESKTOP`), porque así persiste Spring Data MongoDB. La API REST expone estos mismos datos en snake_case y con el tipo en minúsculas (`ram_gb`, `"desktop"`), pero esa conversión ocurre en la capa Jackson del backend, no en la base.

#### 4.2.3 Scripts de la colección (`migraciones/mongodb/`)

| Archivo | Propósito |
|---|---|
| `V1__create_maquina_collection.js` | Creación inicial de la colección con validador (esquema v1, histórico) |
| `V2__update_maquina_schema.js` | Actualiza el validador `$jsonSchema` al modelo vigente (objetos embebidos) vía `collMod` |
| `V3__seed_maquina_documents.js` | Inserta los documentos de hardware de las máquinas seed de SQL, con estructuras variadas |
| `documentos_maquina.json` | Los documentos seed en formato JSON plano (importable con `mongoimport`) |
| `demo_crud.js` | Demo re-ejecutable de las operaciones de la consigna: inserts con estructuras variadas, búsquedas filtradas (campos embebidos, `$exists`, regex), `updateOne`/`updateMany` y `deleteOne`/`deleteMany` |

Los scripts se ejecutan manualmente desde la línea de comandos contra la shell del contenedor, tal como exige la consigna:

```bash
docker exec -i egi-mongodb mongosh inventario_egi --quiet < migraciones/mongodb/V2__update_maquina_schema.js
docker exec -i egi-mongodb mongosh inventario_egi --quiet < migraciones/mongodb/V3__seed_maquina_documents.js
docker exec -i egi-mongodb mongosh inventario_egi --quiet < migraciones/mongodb/demo_crud.js
```

También es posible abrir una shell interactiva dentro del contenedor para ejecutar queries manualmente: `docker exec -it egi-mongodb mongosh inventario_egi`.

---

## 5. Flujo de la Aplicación

### 5.1 Flujograma General de la Aplicación Web

```mermaid
flowchart TD
    START([Inicio]) --> LOGIN[El usuario accede\na la aplicación web]
    LOGIN --> AUTH{Autenticación\nvía LDAP}
    AUTH -- Credenciales inválidas --> ERR1[Mostrar error\nde autenticación]
    ERR1 --> LOGIN
    AUTH -- Autenticado --> DASH[Panel principal\ndel inventario]

    DASH --> OPC{Selecciona\noperación}

    OPC --> BUSCAR[Buscar Máquina\npor ID o filtro]
    OPC --> AGREGAR[Agregar nueva\nmáquina]
    OPC --> EDITAR[Editar máquina\nexistente]
    OPC --> ELIMINAR[Eliminar registro]
    OPC --> ASIGNAR[Asignar máquina\na persona]

    BUSCAR --> QSQL1[Consulta SQL Server:\nubicación + asignación]
    QSQL1 --> QMONGO[Consulta MongoDB:\nhardware por maquina_id]
    QMONGO --> VISTA[Mostrar vista\nunificada al usuario]
    VISTA --> DASH

    AGREGAR --> FSQL[Insertar en SQL Server:\nMaquina + ubicación]
    FSQL --> FMONGO[Insertar documento\nen MongoDB]
    FMONGO --> OK1[Confirmación exitosa]
    OK1 --> DASH

    EDITAR --> USQL[Actualizar SQL Server]
    USQL --> UMONGO[Actualizar MongoDB]
    UMONGO --> OK2[Confirmación exitosa]
    OK2 --> DASH

    ELIMINAR --> CONFIRM{¿Confirmar\neliminación?}
    CONFIRM -- No --> DASH
    CONFIRM -- Sí --> DSQL[Eliminar de SQL Server]
    DSQL --> DMONGO[Eliminar de MongoDB]
    DMONGO --> OK3[Confirmación exitosa]
    OK3 --> DASH

    ASIGNAR --> SELPERSONA[Seleccionar persona]
    SELPERSONA --> INSERTA_REL[Insertar en PersonaMaquina\nSQL Server]
    INSERTA_REL --> OK4[Confirmación exitosa]
    OK4 --> DASH

    DASH --> LOGOUT[Cerrar sesión]
    LOGOUT --> END([Fin])
```

---

## 6. Seguridad y Políticas de Red

### 6.1 Modelo Zero-Trust con NetworkPolicies de Kubernetes

El clúster implementa un modelo de **denegación por defecto**: todo el tráfico dentro del namespace `inventario-seguro` está bloqueado salvo que sea explícitamente permitido por una NetworkPolicy. Las reglas definidas son:

```mermaid
graph LR
    EXT([Tráfico externo\nInternet/Intranet])
    IC["Ingress Controller"]
    FE["inventario-web\n(Frontend + API)"]
    SQL["ubicacion-db\nSQL Server :1433"]
    MONGO["inventario-db\nMongoDB :27017"]
    LDAP["ldap-service\nLDAP :389/:636"]

    EXT -- "HTTPS (permitido\npor Firewall/pfSense)" --> IC
    IC -- "HTTP :8080\n✅ Permitido" --> FE
    FE -- "JDBC :1433\n✅ Permitido" --> SQL
    FE -- "Wire :27017\n✅ Permitido" --> MONGO
    FE -- "LDAP :389\n✅ Permitido" --> LDAP

    SQL -. "❌ Denegado\n(no puede iniciar\ncomunicación)" .-> FE
    MONGO -. "❌ Denegado" .-> FE
    SQL -. "❌ Denegado" .-> MONGO
    LDAP -. "❌ Denegado\nhacia DBs" .-> SQL
```

### 6.2 Reglas de Red Resumidas

| Origen | Destino | Puerto | Acción |
|---|---|---|---|
| Ingress Controller | `inventario-web` | 8080 | ✅ Permitido |
| `inventario-web` | `ubicacion-db` | 1433 | ✅ Permitido |
| `inventario-web` | `inventario-db` | 27017 | ✅ Permitido |
| `inventario-web` | `ldap-service` | 389 / 636 | ✅ Permitido |
| Cualquier otro pod | `ubicacion-db` | 1433 | ❌ Denegado |
| Cualquier otro pod | `inventario-db` | 27017 | ❌ Denegado |
| Cualquier otro pod | `ldap-service` | 389/636 | ❌ Denegado |
| Tráfico sin autorización de DMZ | Ingress | — | ❌ Denegado (Firewall) |

### 6.3 Simulación Perimetral

Se utiliza **GUFW** (interfaz gráfica de UFW) para simular el comportamiento del firewall perimetral universitario. Las reglas configuradas permiten únicamente tráfico HTTPS entrante hacia la IP asignada por Minikube, emulando el NAT del pfSense institucional. Todo tráfico no explícitamente autorizado es bloqueado en esta capa antes de alcanzar el clúster.

---

## 7. Infraestructura y Despliegue

### 7.1 Estructura de Manifiestos Kubernetes

```mermaid
graph TD
    K8S["Clúster Minikube\n(CNI: Calico)"]
    
    K8S --> NS["Namespace:\ninventario-seguro"]
    
    NS --> DEP1["Deployment:\ninventario-web"]
    NS --> DEP2["Deployment:\nubicacion-db"]
    NS --> DEP3["Deployment:\ninventario-db"]
    NS --> DEP4["Deployment:\nldap-service"]

    NS --> SVC1["Service:\ninventario-web\n(NodePort/LoadBalancer)"]
    NS --> SVC2["Service:\nubicacion-db\n(ClusterIP)"]
    NS --> SVC3["Service:\ninventario-db\n(ClusterIP)"]
    NS --> SVC4["Service:\nldap-service\n(ClusterIP)"]

    NS --> NP1["NetworkPolicy:\ndefault-deny-all"]
    NS --> NP2["NetworkPolicy:\nallow-frontend-egress"]
    NS --> NP3["NetworkPolicy:\nallow-db-from-frontend"]
    NS --> NP4["NetworkPolicy:\nallow-ldap-from-frontend"]

    NS --> PVC1["PVC: sqlserver-data"]
    NS --> PVC2["PVC: mongodb-data"]
    NS --> PVC3["PVC: ldap-data"]

    NS --> CM1["ConfigMap:\napp-config"]
    NS --> SEC1["Secret:\ndb-credentials"]
    NS --> SEC2["Secret:\nldap-credentials"]
```

### 7.2 Consideraciones de Despliegue

- El clúster debe iniciarse con Calico como CNI para que las NetworkPolicies tengan efecto efectivo:
  `minikube start --cni=calico`
- Los PersistentVolumeClaims garantizan la persistencia de datos de las bases de datos y del directorio LDAP ante reinicios de los pods.
- Las credenciales de acceso a las bases de datos y al servidor LDAP se gestionan mediante **Secrets de Kubernetes**, nunca como variables de entorno en texto plano dentro de los Deployments.
- El frontend (`inventario-web`) es el único servicio expuesto externamente mediante un Service de tipo `NodePort` o a través del Ingress Controller.
- **SQL Server (`ubicacion-db`)** corre en una **máquina virtual dedicada**, fuera del clúster y fuera de Docker Compose de producción. El backend se conecta mediante `DB_URL` (ver sección 11.8).
- Para desarrollo y pruebas locales, SQL Server puede levantarse temporalmente con `docker-compose.dev.yml`; en producción solo se dockerizan frontend, backend y MongoDB (`docker-compose.yml`).

---

## 8. Estructura del Repositorio Git

El proyecto debe versionarse en un repositorio Git unificado con la siguiente estructura de directorios sugerida:

```
/
├── README.md
├── .env.example              # Plantilla de variables para docker compose (producción)
├── docker-compose.yml          # Frontend + backend + MongoDB (SQL Server en VM externa)
├── docker-compose.dev.yml      # SQL Server + MongoDB para desarrollo local
├── docker/
│   └── sqlserver/
│       └── init.sql            # Creación de BD para compose de desarrollo
├── docs/
│   ├── arquitectura.md
│   ├── diagrama_clases.png
│   ├── diagrama_topologia.png
│   └── flujograma_app.mmd
├── k8s/
│   ├── namespace.yaml
│   ├── deployments/
│   │   ├── inventario-web.yaml
│   │   ├── ubicacion-db.yaml
│   │   ├── inventario-db.yaml
│   │   └── ldap-service.yaml
│   ├── services/
│   │   └── *.yaml
│   ├── network-policies/
│   │   ├── default-deny-all.yaml
│   │   ├── allow-frontend.yaml
│   │   └── allow-db-from-frontend.yaml
│   └── storage/
│       └── pvc.yaml
├── app/
│   ├── inventario-web/
│   │   ├── Dockerfile          # Backend Spring Boot (multi-stage)
│   │   ├── frontend/
│   │   │   ├── Dockerfile      # Frontend React (nginx)
│   │   │   └── src/
│   │   └── src/
├── migraciones/
│   ├── sql/
│   │   └── V0__create_database.sql  # Script para VM de SQL Server
│   └── mongodb/
└── firewall/
    └── reglas_gufw.md
```

El historial de commits debe reflejar la contribución individual de cada integrante y la evolución incremental del proyecto, siguiendo buenas prácticas de mensajes descriptivos y ramas de trabajo por funcionalidad.

---

## 9. División de Tareas del Equipo

| Integrante | Responsabilidad Principal |
|---|---|
| Integrante 1 | Diseño de arquitectura, diagrama de topología, manifiestos Kubernetes (Deployments y Services) |
| Integrante 2 | Modelado y creación de la base de datos SQL Server (`ubicacion-db`), script SQL |
| Integrante 3 | Diseño de colección MongoDB (`inventario-db`), documentos JSON, queries de prueba |
| Integrante 4 | Desarrollo de la aplicación web (`inventario-web`): frontend + API REST (Spring Boot) |
| Integrante 5 | Configuración del servidor LDAP, NetworkPolicies, simulación de firewall (GUFW), presentación |

---

## 10. Entregables del Proyecto

1. **Documentación técnica**: Informe con esquema de arquitectura, diagrama de clases, diagrama de topología, flujograma y reglas de red.
2. **Repositorio Git**: Código fuente versionado de la aplicación web, Dockerfiles, manifiestos Kubernetes, scripts SQL y documentos JSON de MongoDB.
3. **Ecosistema funcional en Minikube**: Todos los servicios levantados y comunicándose correctamente dentro del namespace, con NetworkPolicies activas.
4. **Aplicación web funcional**: Permite gestionar el inventario de aulas (CRUD completo, búsqueda, asignación de máquinas a personas).
5. **Presentación**: Diapositivas en formato PowerPoint o compatible, estructuradas para una defensa de 17 minutos por equipo.

---

## 11. Implementación del Backend

### 11.1 Estructura del proyecto Spring Boot

El backend se implementa como un monolito modular bajo `app/inventario-web/`, utilizando **Spring Boot 3.4.1** con **Java 17** y **Maven** como herramienta de construcción.

```
app/inventario-web/
├── pom.xml
└── src/main/java/com/itu/egi/inventarioseguro/
    ├── config/         DataSourceConfig — registra repositorios JPA y MongoDB en paquetes separados
    ├── model/          Entidades JPA, documento MongoDB y enums
    ├── repository/
    │   ├── sql/        Repositorios Spring Data JPA (SQL Server)
    │   └── mongo/      Repositorios Spring Data MongoDB
    ├── dto/            Objetos de transferencia de datos (request y response)
    ├── service/        Lógica de negocio
    └── controller/     Controladores REST (/api/*)
```

### 11.2 Dependencias principales

| Dependencia | Propósito |
|---|---|
| `spring-boot-starter-web` | API REST con Jackson |
| `spring-boot-starter-data-jpa` | ORM sobre SQL Server con Hibernate |
| `spring-boot-starter-data-mongodb` | Repositorios sobre MongoDB |
| `mssql-jdbc` | Driver JDBC para SQL Server |
| `flyway-core` + `flyway-sqlserver` | Migraciones automáticas al arrancar |
| `spring-boot-starter-validation` | Validación de DTOs con Jakarta Bean Validation |
| `lombok` | Reducción de boilerplate (getters, setters, constructores) |

### 11.3 Modelo de dominio

#### Entidades JPA (SQL Server)

- **`Persona`** — mapea la tabla `persona`. Categoría como enum `Categoria` (`RESPONSABLE_TECNICO`, `ALUMNO`, `DOCENTE`).
- **`Maquina`** — mapea la tabla `maquina`. Aula como enum `Aula` (`AULA_1y2`, `LABORATORIO_SO`, `LABORATORIO_REDES`, `AULA_4`).
- **`PersonaMaquina`** — mapea la tabla `persona_maquina`. Usa `@EmbeddedId` con `PersonaMaquinaId` por tener el campo adicional `fecha_asignado`.

#### Documento MongoDB

- **`MaquinaHardware`** — colección `maquina`. Su `_id` es el mismo `Long` que el `id` de la entidad `Maquina` en SQL Server, funcionando como clave compartida entre bases.

### 11.4 Endpoints REST

Todos los endpoints devuelven y aceptan JSON con naming en **snake_case** (configurable globalmente via Jackson `SNAKE_CASE`).

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Autenticación — devuelve `{username, token, role}` |
| GET | `/api/maquinas` | Lista completa: SQL + MongoDB + asignaciones combinados |
| GET | `/api/maquinas/{id}` | Detalle unificado por ID |
| POST | `/api/maquinas` | Crea máquina en SQL y su hardware en MongoDB, con asignaciones |
| PUT | `/api/maquinas/{id}` | Actualiza en ambas bases y reemplaza asignaciones |
| DELETE | `/api/maquinas/{id}` | Elimina de ambas bases |
| GET | `/api/personas` | Lista todas las personas |
| GET | `/api/personas/{id}` | Detalle de persona |
| POST | `/api/personas` | Crea persona |
| PUT | `/api/personas/{id}` | Actualiza persona |
| DELETE | `/api/personas/{id}` | Elimina persona |
| POST | `/api/asignaciones` | Asigna máquina a persona (standalone) |
| DELETE | `/api/asignaciones/{personaId}/{maquinaId}` | Desasigna |

#### Estructura de request/response de `/api/maquinas`

**GET /api/maquinas** — respuesta (array de):
```json
{
  "id": 1,
  "numero_mesa": 5,
  "fecha_mantenimiento": "2026-12-01",
  "aula": "LABORATORIO_SO",
  "especificaciones": {
    "maquina_id": 1,
    "fabricante": "Dell",
    "modelo": "OptiPlex 7090",
    "tipo": "desktop",
    "cpu": "Intel Core i5-11500",
    "ram_gb": 16,
    "disco": { "tipo": "SSD", "capacidad_gb": 512 },
    "sistema_operativo": "Windows 11 Pro",
    "perifericos": { "monitor": "Dell 24\"", "mouse": "Dell MS116", "teclado": "Dell KB216" }
  },
  "asignaciones": [
    {
      "persona": { "id": 1, "nombre": "Juan", "apellido": "Humeniuk", "categoria": "Docente" },
      "fecha_asignado": "2026-06-06"
    }
  ]
}
```

**POST /api/maquinas** — body esperado:
```json
{
  "aula": "LABORATORIO_SO",
  "numero_mesa": 5,
  "fecha_mantenimiento": "2026-12-01",
  "especificaciones": {
    "fabricante": "Dell",
    "modelo": "OptiPlex 7090",
    "tipo": "desktop",
    "cpu": "Intel Core i5-11500",
    "ram_gb": 16,
    "disco": { "tipo": "SSD", "capacidad_gb": 512 },
    "sistema_operativo": "Windows 11 Pro",
    "perifericos": { "monitor": "Dell 24\"", "mouse": "Dell MS116", "teclado": "Dell KB216" }
  },
  "asignaciones": [
    { "personaId": 1, "fecha_asignado": "2026-06-06" }
  ]
}
```

### 11.5 Gestión de migraciones (Flyway)

Al arrancar la aplicación, Flyway ejecuta automáticamente las migraciones en orden:

| Versión | Acción |
|---|---|
| V1 | Crea tabla `persona` con CHECK en `categoria` |
| V2 | Crea tabla `maquina` |
| V3 | Crea tabla `persona_maquina` (N:M) con índice en `maquina_id` |
| V4 | Elimina columna `laboratorio` y agrega CHECK enum `aula` |

### 11.6 Configuración de doble fuente de datos (JPA + MongoDB)

La coexistencia de Spring Data JPA y Spring Data MongoDB en la misma aplicación requiere una configuración explícita para evitar conflictos en la detección automática de repositorios. Esto se implementa en la clase `DataSourceConfig`:

```java
@Configuration
@EnableJpaRepositories(basePackages = "...repository.sql")
@EnableMongoRepositories(basePackages = "...repository.mongo")
public class DataSourceConfig {
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        MongoTemplate template = new MongoTemplate(factory);
        template.setSessionSynchronization(SessionSynchronization.NEVER);
        return template;
    }
}
```

El ajuste `SessionSynchronization.NEVER` es necesario para que Spring Data MongoDB no intente enlazar sus operaciones de escritura al ciclo de vida de las transacciones JPA activas (que gestionan SQL Server), lo que de otro modo causaría que las escrituras a MongoDB se descarten silenciosamente.

### 11.7 Entornos Docker

El repositorio incluye dos archivos Compose con propósitos distintos:

#### Desarrollo local — `docker-compose.dev.yml`

Levanta **solo las bases de datos** para trabajar con el backend y el frontend fuera de contenedores (IDE, `mvn spring-boot:run`, `npm run dev`):

| Servicio | Imagen | Puerto host | Credenciales |
|---|---|---|---|
| SQL Server 2022 | `mcr.microsoft.com/mssql/server:2022-latest` | 1433 | sa / `EGI_Password123!` |
| MongoDB 7 | `mongo:7` | **27018** | Sin autenticación |

Incluye un job `sqlserver-init` que ejecuta `docker/sqlserver/init.sql` para crear la base `inventario_egi` en el primer arranque.

MongoDB se expone en el **puerto 27018** (no 27017) para evitar conflictos con instalaciones locales del servicio MongoDB.

```bash
docker compose -f docker-compose.dev.yml up -d
```

#### Despliegue — `docker-compose.yml`

Levanta **frontend**, **backend** y **MongoDB** con builds de producción (nginx + JAR). **SQL Server no se dockeriza**: corre en una **VM externa** y el backend se conecta mediante variables de entorno.

| Servicio | Dónde corre | Puerto (ejemplo) |
|---|---|---|
| Frontend (nginx) | Contenedor Docker | 3000 → 80 |
| Backend (Spring Boot) | Contenedor Docker | 8080 |
| MongoDB | Contenedor Docker | red interna `mongodb:27017` |
| SQL Server | **VM externa** | 1433 (configurado en `DB_URL`) |

```bash
cp .env.example .env
# Editar .env con la IP/hostname de la VM de SQL Server
docker compose up --build -d
```

##### Preparar la VM de SQL Server

1. Instalar SQL Server en la VM y abrir el puerto **1433** hacia el host donde corre Docker.
2. Crear la base de datos (una sola vez):

```bash
sqlcmd -S localhost -U sa -P '<password>' -C -i migraciones/sql/V0__create_database.sql
```

3. Configurar la conexión en `.env` (ver sección 11.8).

Flyway aplica las migraciones V1–V4 automáticamente al arrancar el backend.

##### Probar el compose de producción sin VM externa

Para desarrollo integrado, levantar SQL Server con el compose de desarrollo y apuntar el backend al host:

```bash
docker compose -f docker-compose.dev.yml up -d
cp .env.example .env
# En .env:
# DB_URL=jdbc:sqlserver://host.docker.internal:1433;databaseName=inventario_egi;encrypt=false;trustServerCertificate=true
# DB_PASSWORD=EGI_Password123!
docker compose up --build -d
```

##### Seed de MongoDB (opcional)

```bash
docker compose exec -T mongodb mongosh inventario_egi --quiet < migraciones/mongodb/V3__seed_maquina_documents.js
```

### 11.8 Configuración de entorno

La aplicación se configura mediante variables de entorno para no hardcodear credenciales. El archivo `.env.example` en la raíz del repositorio documenta todas las variables del despliegue Docker; copiarlo a `.env` antes de `docker compose up`.

#### Backend (Spring Boot)

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=inventario_egi;...` | URL JDBC completa hacia SQL Server. En producción apunta a la VM externa |
| `DB_USER` | `sa` | Usuario SQL Server |
| `DB_PASSWORD` | `sa` | Contraseña SQL Server |
| `MONGO_URI` | `mongodb://localhost:27018/inventario_egi` | URI MongoDB. En Docker: `mongodb://mongodb:27017/inventario_egi` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Orígenes permitidos para CORS (URL pública del frontend) |

En `application.yml`, la URL de SQL Server se resuelve desde `DB_URL`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=inventario_egi;encrypt=false;trustServerCertificate=true}
    username: ${DB_USER:sa}
    password: ${DB_PASSWORD:sa}
```

#### Frontend (build Docker)

Estas variables se inyectan en **build time** mediante build args del `docker-compose.yml`:

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080/api` | URL pública de la API (desde el navegador del usuario) |
| `VITE_USE_MOCK` | `false` | Si es `true`, el frontend usa datos mock en localStorage sin backend |

Para desarrollo local con Vite (`npm run dev`), las mismas variables se definen en `app/inventario-web/frontend/.env.local`.

---

## 12. Frontend — Aplicación React

### 12.1 Tecnologías

La interfaz web está implementada con **React 19 + TypeScript + Vite + Tailwind CSS**, ubicada en `app/inventario-web/frontend/`.

| Tecnología | Versión | Rol |
|---|---|---|
| React + TypeScript | 19 / 5.8 | UI y tipado estático |
| Vite | 6 | Build tool y dev server (puerto 3000) |
| Tailwind CSS | 4 | Estilos utilitarios |
| Lucide React | 0.546 | Iconografía |

### 12.2 Arquitectura de servicios

El frontend implementa una capa de servicios en `src/services/` que abstrae la comunicación con el backend:

| Archivo | Responsabilidad |
|---|---|
| `api.ts` | Cliente base HTTP con inyección automática de JWT en el header `Authorization: Bearer <token>` |
| `authService.ts` | Login contra `/api/auth/login`, persistencia del token en `localStorage` |
| `maquinaService.ts` | CRUD completo contra `/api/maquinas` y `/api/personas` |

### 12.3 Modo Mock vs. Real

El frontend soporta dos modos controlados por variables de entorno:

```env
# Modo real (conecta al backend Spring Boot)
VITE_API_URL=http://localhost:8080/api
VITE_USE_MOCK=false

# Modo mock (datos en localStorage, sin backend)
VITE_USE_MOCK=true
```

| Archivo | Cuándo usarlo |
|---|---|
| `app/inventario-web/frontend/.env.local` | Desarrollo local con `npm run dev` |
| `.env` (raíz del repo) | Despliegue con `docker compose up` (build args del frontend y env del backend) |

Ver `.env.example` en la raíz del repositorio para la plantilla completa de despliegue.

El modo mock simula con fidelidad el comportamiento del backend (transacciones SQL + MongoDB) usando `localStorage`, lo que permite desarrollar el frontend de forma independiente.

### 12.4 Levantar el frontend

```bash
cd app/inventario-web/frontend
npm install
npm run dev        # http://localhost:3000
```

Credenciales por defecto para el login: `admin` / `admin123` (o cualquier usuario/contraseña mientras el LDAP real esté pendiente).

---

## 13. Integración Backend-Frontend

### 13.1 CORS

La clase `CorsConfig` (en `config/`) habilita CORS para que el frontend pueda comunicarse con el backend. Los orígenes permitidos se configuran con la variable de entorno `CORS_ALLOWED_ORIGINS` (por defecto `http://localhost:3000`):

```java
@Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000}")
private String[] allowedOrigins;

registry.addMapping("/api/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
```

En despliegue Docker, `CORS_ALLOWED_ORIGINS` debe coincidir con la URL pública desde la que se sirve el frontend.

### 13.2 Convención de naming JSON

Jackson está configurado globalmente con la estrategia `SNAKE_CASE` en `application.yml`:

```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

Esto convierte automáticamente los campos camelCase de Java (`numeroMesa`, `ramGb`, `sistemaOperativo`) a snake_case en el JSON (`numero_mesa`, `ram_gb`, `sistema_operativo`), alineándose con la convención del frontend.

### 13.3 Autenticación (mock JWT)

El endpoint `POST /api/auth/login` devuelve un token de sesión. La integración con el servidor LDAP real (OpenLDAP / Active Directory) está pendiente; por ahora, el endpoint acepta cualquier credencial y retorna un token aleatorio:

```json
{
  "username": "admin",
  "token": "mock-jwt-uuid-generado",
  "role": "ADMIN"
}
```

El rol se determina según el username: `admin` → `ADMIN`, cualquier otro → `TECNICO`.

### 13.4 Validación del contrato de API

La integración fue verificada con Playwright (Chromium headless):

| Check | Resultado |
|---|---|
| Login con `admin/admin123` | ✅ `POST /api/auth/login → 200` |
| Dashboard carga máquinas reales | ✅ `GET /api/maquinas → 200` |
| Lista de personas para selector | ✅ `GET /api/personas → 200` |
| Crear máquina desde formulario | ✅ `POST /api/maquinas → 201` |
| Errores CORS en consola del navegador | ✅ 0 errores |
| Errores de JavaScript en consola | ✅ 0 errores |

---

## 14. Tests

### 14.1 Estrategia de testing

Se implementaron **17 tests** divididos en tres niveles, sin requerir bases de datos externas:

| Tipo | Clase | Tests | Herramienta |
|---|---|---|---|
| Unitario (service) | `MaquinaServiceTest` | 7 | JUnit 5 + Mockito |
| Integración HTTP (controller) | `MaquinaControllerTest` | 6 | `@WebMvcTest` + MockMvc |
| Integración HTTP (auth) | `AuthControllerTest` | 3 | `@WebMvcTest` + MockMvc |
| Placeholder | `InventarioApplicationTests` | 1 | — |

### 14.2 Casos cubiertos

**MaquinaServiceTest** — lógica de negocio:
- `findAll` devuelve lista con datos de SQL + MongoDB combinados
- `findAll` con MongoDB vacío devuelve especificaciones en null sin crashear
- `findById` happy path con datos completos
- `findById` ID inexistente → 404
- `create` persiste en SQL Server Y en MongoDB
- `delete` elimina de ambas bases
- `delete` ID inexistente → 404 sin tocar las bases

**MaquinaControllerTest** — capa HTTP:
- `GET /api/maquinas` → 200 con JSON en snake_case (`numero_mesa`, `ram_gb`, `sistema_operativo`, `capacidad_gb`)
- `GET /api/maquinas/{id}` → 200 con estructura completa
- `POST /api/maquinas` con body válido → 201
- `POST /api/maquinas` sin `especificaciones` (@NotNull) → 400
- `PUT /api/maquinas/{id}` → 200
- `DELETE /api/maquinas/{id}` → 204

**AuthControllerTest** — autenticación:
- Usuario `admin` → role `ADMIN`
- Cualquier otro usuario → role `TECNICO`
- Body vacío → respuesta 200 con token

### 14.3 Ejecutar los tests

```bash
cd app/inventario-web
mvn test
# BUILD SUCCESS — Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
```

Los tests de controller (`@WebMvcTest`) y los unitarios (Mockito) no requieren Docker ni bases de datos.

---

## 15. Conclusión

El proyecto EGI representa un ejercicio integral de ingeniería de sistemas, abarcando desde el análisis y diseño de bases de datos relacionales y documentales, hasta la implementación de seguridad perimetral, autenticación centralizada y orquestación de contenedores. La arquitectura propuesta es escalable, segura por diseño y alineada con los principios modernos de infraestructura como código y Zero-Trust Networking.

La distribución del trabajo en cinco roles complementarios garantiza que el equipo aborde todas las capas del ecosistema, y la defensa individual de cada parte refuerza la apropiación del conocimiento por parte de cada integrante.

---

*Documento generado para el Proyecto Integrador de la asignatura EGI — Instituto Tecnológico Universitario*
