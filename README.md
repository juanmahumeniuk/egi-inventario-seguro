<div align="center">

# Ecosistema de Inventario Seguro

**Proyecto Integrador · EGI · ITU**

Sistema centralizado para inventariar equipos de laboratorios de informática, con despliegue contenerizado, políticas de red Zero-Trust y autenticación institucional.

<br/>

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![SQL Server](https://img.shields.io/badge/SQL%20Server-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white)](https://www.microsoft.com/sql-server)
[![LDAP](https://img.shields.io/badge/LDAP-005571?style=for-the-badge&logo=openldap&logoColor=white)](https://www.openldap.org/)

</div>

---

## Descripción

Sistema web que permite inventariar y gestionar los equipos de los laboratorios del ITU. Combina datos de ubicación/asignación (SQL Server) con datos de hardware (MongoDB) en una API REST, desplegada sobre Kubernetes con autenticación LDAP y políticas de red Zero-Trust.

Consulta el documento completo del enunciado en [`Proyecto Integrador EGI.md`](./Proyecto%20Integrador%20EGI.md) y el informe técnico en [`docs/Informe_EGI_Inventario.md`](./docs/Informe_EGI_Inventario.md).

---

## Arquitectura

```mermaid
flowchart TB
    subgraph Perimetro["Perímetro (GUFW / pfSense)"]
        FW[Firewall / DMZ]
    end

    subgraph K8s["Namespace · Minikube"]
        WEB["inventario-web<br/><i>Frontend</i>"]
        SQL["ubicacion-db<br/><i>SQL Server / MySQL</i>"]
        MONGO["inventario-db<br/><i>MongoDB</i>"]
        LDAP["ldap-service<br/><i>Active Directory / LDAP</i>"]
    end

    USER((Usuario)) --> FW
    FW -->|HTTP| WEB
    WEB --> SQL
    WEB --> MONGO
    WEB --> LDAP
```

| Servicio | Rol | Puerto |
|----------|-----|--------|
| `inventario-web` | Interfaz y lógica de aplicación | _Por definir_ |
| `ubicacion-db` | Ubicación, responsables, mantenimiento | _Por definir_ |
| `inventario-db` | Hardware y componentes internos | _Por definir_ |
| `ldap-service` | Autenticación institucional | _Por definir_ |

<!-- TODO: Diagrama de red, NetworkPolicies y reglas de firewall -->

---

## Stack tecnológico

| Capa | Tecnologías |
|------|-------------|
| **Backend** | Spring Boot, Java |
| **Frontend** | _Por definir_ |
| **Datos** | SQL Server / MySQL, MongoDB |
| **Identidad** | Active Directory / LDAP |
| **Infraestructura** | Docker, Kubernetes (Minikube), Calico |
| **Seguridad** | Network Policies, GUFW / pfSense |

---

## Estructura del repositorio

```
egi-inventario-seguro/
├── app/
│   └── inventario-web/           # Spring Boot (API REST + Frontend)
│       ├── pom.xml
│       └── src/
│           ├── main/
│           │   ├── java/com/itu/egi/inventarioseguro/
│           │   │   ├── config/   # DataSourceConfig (JPA + MongoDB)
│           │   │   ├── model/    # Entidades JPA, documento MongoDB, enums
│           │   │   ├── repository/sql/    # Repos JPA
│           │   │   ├── repository/mongo/  # Repos MongoDB
│           │   │   ├── dto/      # DTOs de request y response
│           │   │   ├── service/  # Lógica de negocio
│           │   │   └── controller/ # Endpoints REST /api/*
│           │   └── resources/
│           │       ├── application.yml
│           │       └── db/migration/  # Flyway V1–V4
│           └── test/
├── docs/                         # Informe, diagramas
├── migraciones/
│   ├── sql/                      # V1–V4 (referencia)
│   └── mongodb/                  # Schema de colección
└── README.md
```

---

## Requisitos previos

- Java 17+
- Maven 3.9+
- Docker Desktop
- Minikube (`minikube start --cni=calico`) y kubectl — para despliegue en clúster
- IntelliJ IDEA (recomendado) u otro IDE compatible con Spring Boot

> **Nota**: el `docker-compose.dev.yml` levanta SQL Server en el puerto **1433** y MongoDB en el **27018** (no 27017, para evitar conflicto con instalaciones locales de MongoDB).

---

## Inicio rápido (desarrollo local)

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio>
cd egi-inventario-seguro

# 2. Levantar las bases de datos con Docker Compose
docker compose -f docker-compose.dev.yml up -d

# 3. Configurar variables de entorno en el IDE
#    DB_PASSWORD=EGI_Password123!
#    (DB_USER y MONGO_URI tienen valores por defecto en application.yml)

# 4. Compilar y ejecutar el backend desde IntelliJ
#    o desde terminal:
cd app/inventario-web
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DDB_PASSWORD=EGI_Password123!"

# 5. La API queda disponible en http://localhost:8080/api
```

---

## Despliegue con Docker

Levanta la **aplicación** (Spring Boot + frontend embebido) y **MongoDB**. **SQL Server corre en una VM externa** (no incluido en este compose).

```bash
cp .env.example .env
# Editar .env: DB_URL y DB_PASSWORD apuntando a la VM de SQL Server
docker compose up --build -d
```

| Servicio | Dónde corre | URL (ejemplo local) |
|----------|-------------|---------------------|
| App (API + UI) | Contenedor Docker | http://localhost:8080 |
| API REST | Mismo contenedor | http://localhost:8080/api |
| MongoDB | Contenedor Docker | red interna (`mongodb:27017`) |
| SQL Server | **VM externa** | configurado en `DB_URL` del `.env` |

El frontend React se compila durante `mvn package` (via `frontend-maven-plugin`) y se sirve como estático desde Spring Boot. No hay contenedor nginx separado.

### Generar las imágenes Docker

| Imagen | Origen | Descripción |
|--------|--------|-------------|
| `almacenamiento-seguro-backend` | [`app/inventario-web/Dockerfile`](app/inventario-web/Dockerfile) | Spring Boot + frontend embebido |
| `mongo:7` | Docker Hub | Base de datos MongoDB |

**Construir todas las imágenes** (sin levantar contenedores):

```bash
cp .env.example .env
docker compose build
```

**Construir la imagen de la aplicación:**

```bash
docker build -t almacenamiento-seguro-backend ./app/inventario-web
```

**Verificar que las imágenes existen:**

```bash
docker images | grep -E "almacenamiento-seguro|mongo"
```

**Exportar para otra máquina** (sin reconstruir en el destino):

```bash
docker save almacenamiento-seguro-backend mongo:7 \
  -o inventario-seguro-images.tar
```

En el servidor destino:

```bash
docker load -i inventario-seguro-images.tar
cp .env.example .env   # configurar y luego: docker compose up -d
```

### Preparar la VM de SQL Server

1. Instalar SQL Server en la VM y abrir el puerto **1433** hacia el host donde corre Docker.
2. Crear la base de datos (una sola vez):

```bash
sqlcmd -S localhost -U sa -P '<password>' -C -i migraciones/sql/V0__create_database.sql
```

3. Configurar en `.env` la conexión hacia la VM:

```env
DB_URL=jdbc:sqlserver://<ip-o-hostname-vm>:1433;databaseName=inventario_egi;encrypt=true;trustServerCertificate=true
DB_USER=sa
DB_PASSWORD=<password>
```

Flyway aplica las migraciones V1–V4 al arrancar el backend.

Documentación detallada en [`docs/Informe_EGI_Inventario.md`](docs/Informe_EGI_Inventario.md) (secciones 11.7 y 11.8).

### Desarrollo local con SQL Server en Docker

Para probar sin VM externa, levanta SQL Server con el compose de desarrollo y apunta el backend al host:

```bash
docker compose -f docker-compose.dev.yml up -d
cp .env.example .env
# En .env, usar:
# DB_URL=jdbc:sqlserver://host.docker.internal:1433;databaseName=inventario_egi;encrypt=false;trustServerCertificate=true
# DB_PASSWORD=EGI_Password123!
docker compose up --build -d
```

### Seed de MongoDB (opcional)

Tras el primer arranque, puedes cargar documentos de ejemplo:

```bash
docker compose exec -T mongodb mongosh inventario_egi --quiet < migraciones/mongodb/V3__seed_maquina_documents.js
```

### Endpoints disponibles

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/auth/login` | Autenticar con LDAP, retorna JWT | ✗ |
| GET | `/api/maquinas` | Listar todas las máquinas | ✓ JWT |
| GET | `/api/maquinas/{id}` | Detalle unificado (SQL + MongoDB) | ✓ JWT |
| POST | `/api/maquinas` | Crear máquina | ✓ EDITOR |
| PUT | `/api/maquinas/{id}` | Actualizar máquina | ✓ EDITOR |
| DELETE | `/api/maquinas/{id}` | Eliminar máquina | ✓ ADMIN |
| GET | `/api/maquinas/{id}/personas` | Personas asignadas | ✓ JWT |
| GET | `/api/personas` | Listar personas | ✓ JWT |
| POST | `/api/personas` | Crear persona | ✓ EDITOR |
| PUT | `/api/personas/{id}` | Actualizar persona | ✓ EDITOR |
| DELETE | `/api/personas/{id}` | Eliminar persona | ✓ ADMIN |
| POST | `/api/asignaciones` | Asignar máquina a persona | ✓ EDITOR |
| DELETE | `/api/asignaciones/{personaId}/{maquinaId}` | Desasignar | ✓ ADMIN |

---

## Despliegue en Kubernetes (Minikube + pfSense)

### Topología de red

```
                 Internet/WAN
                      |
                  pfSense (VM1)
                      |
            Red interna "egi-lan" (192.168.1.0/24)
         /              |              |              \
    Windows-AD      Windows-SQL    Ubuntu VM4      pfSense WAN
    (192.168.1.10)  (192.168.1.20) (192.168.1.40)  (VirtualBox NAT)
    LDAP (389)      SQL (1433)      Minikube
    DNS (53)                        NodePort 30443
```

**Flujo de request externo:**
```
Cliente WAN → pfSense WAN:443 → [NAT] → Ubuntu:30443 → ingress-nginx → inventario-web:8080
```

### Configuración de Ubuntu VM (Minikube + Calico)

1. **SSH a la VM**:
   ```bash
   ssh -p 2222 gonza2001@<WINDOWS_HOST_IP>  # VirtualBox port forward
   ```

2. **Iniciar Minikube CON Calico** (crítico: CNI debe estar en la creación):
   ```bash
   minikube start --cni=calico --driver=docker --memory=4096 --cpus=2
   minikube addons enable ingress
   ```

3. **Aplicar manifiestos** (orden: config → datos → app → red):
   ```bash
   kubectl apply -f k8s/namespace.yaml
   kubectl apply -f k8s/configmap.yaml
   
   # Crear los Secrets (NO están en el repo)
   kubectl -n inventario-seguro create secret generic sql-secret \
     --from-literal=DB_PASSWORD='Itu12345!'
   kubectl -n inventario-seguro create secret generic ldap-secret \
     --from-literal=LDAP_BIND_DN='cn=Administrador,cn=Users,dc=itu,dc=local' \
     --from-literal=LDAP_BIND_PASSWORD='Itu12345!'
   kubectl -n inventario-seguro create secret generic mongo-secret
   kubectl -n inventario-seguro create secret generic jwt-secret \
     --from-literal=JWT_SECRET='SecretKeyForEGIIinventarioSeguroNeedsToBeAtLeast32BytesLong...'
   
   # Storage + MongoDB
   kubectl apply -f k8s/storage/mongodb-pvc.yaml -f k8s/deployments/mongodb.yaml -f k8s/services/mongodb.yaml
   
   # App (esperar a que MongoDB esté Ready)
   kubectl apply -f k8s/deployments/inventario-web.yaml -f k8s/services/inventario-web.yaml
   
   # Ingress + NodePort para pfSense
   kubectl apply -f k8s/ingress/inventario-ingress.yaml
   kubectl apply -f k8s/ingress/ingress-nginx-nodeport.yaml
   
   # NetworkPolicies (denegación total + 4 permisos, JUNTOS)
   kubectl apply -f k8s/network-policies/
   ```

4. **Exponer el NodePort externamente** (systemd service en Ubuntu):
   ```bash
   sudo tee /etc/systemd/system/egi-portforward.service > /dev/null <<'EOF'
   [Unit]
   Description=EGI Inventario - kubectl port-forward
   After=docker.service network-online.target
   Wants=docker.service
   
   [Service]
   User=gonza2001
   Environment=HOME=/home/gonza2001
   ExecStartPre=/bin/bash -c "minikube status | grep -q Running || minikube start --cni=calico --driver=docker"
   ExecStartPre=/bin/sleep 15
   ExecStart=/usr/local/bin/kubectl port-forward -n ingress-nginx svc/ingress-nginx-pfsense-nodeport 30443:443 --address=0.0.0.0
   Restart=always
   RestartSec=15
   
   [Install]
   WantedBy=multi-user.target
   EOF
   sudo systemctl daemon-reload && sudo systemctl enable --now egi-portforward
   ```

### Configuración de pfSense (VM1)

1. **Interfaces**:
   - **WAN**: Adaptador puente o NAT (salida a internet)
   - **LAN**: Red interna `egi-lan`, IP `192.168.1.1/24`

2. **DHCP + Outbound NAT** (ya configurado):
   - DHCP en LAN: `192.168.1.100`–`192.168.1.200`
   - Outbound NAT: enmascara `192.168.1.0/24` por WAN

3. **Bloqueo de redes privadas**: DESHABILITADO en WAN (permite NAT desde redes privadas)

4. **Port Forward** (lo importante):
   - Interface: **WAN**
   - Protocolo: **TCP**
   - Destino: **WAN address**
   - Puerto destino: **443**
   - Redirect IP: **192.168.1.40** (Ubuntu VM4)
   - Redirect puerto: **30443**
   - Crear regla de firewall asociada: ✓

5. **Reglas de Firewall**:
   - **WAN**: Solo TCP 443 (del port forward). El resto denegado → default-deny perimetral
   - **LAN**: Por defecto "allow LAN to any" (las NetworkPolicies de Calico acotarán internamente)

### Configuración de VM SQL Server y AD

- **SQL Server** (192.168.1.20:1433): Base de datos `inventario_egi`, usuario `sa`
- **Windows-AD** (192.168.1.10:389): LDAP, usuarios en `OU=Users,OU=ITU,DC=itu,DC=local`
- **DNS**: Apunta a Windows-AD (192.168.1.10)

### Verificación end-to-end

```bash
# 1. Desde Ubuntu VM4: conectividad a servicios externos
kubectl exec -it <pod-inventario-web> -n inventario-seguro -- \
  curl -k https://inventario.itu.local:30443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"osmelmata","password":"Itu12345!"}'

# 2. Desde Windows-AD o host cliente: test del port forward de pfSense
curl -k -H "Host: inventario.itu.local" https://192.168.0.136:30443/api/auth/login

# 3. En browser: https://inventario.itu.local:30443 (con hosts entry)

# 4. NetworkPolicies: verificar que pods NO pueden alcanzar servicios no autorizados
kubectl logs -n inventario-seguro deployment/inventario-web | grep -i ldap
```

> **Puntos críticos:**
> 1. **Calico OBLIGATORIO** en `minikube start --cni=calico`. Agregarlo después no funciona.
> 2. **NodePort 30443 debe ser accesible** en la IP de Ubuntu (192.168.1.40). `kubectl port-forward --address=0.0.0.0` lo expone correctamente.
> 3. **pfSense desbloquea redes privadas en WAN** (sin esto, NAT no funciona).
> 4. **Secrets no están en el repo** (seguridad). Crearlos a mano antes de desplegar la app.
> 5. **CORS** configurado dinámicamente desde `CORS_ALLOWED_ORIGINS` en configmap (ver `SecurityConfig.java`).
> 6. **NetworkPolicies** (`default-deny-all` + 4 allow rules) se aplican JUNTAS. Una sola genera timeout en el 30443.

---

## Estado actual (2026-06-21)

✅ **Implementación completada y verificada end-to-end:**

- ✅ API REST Spring Boot + Frontend React compilado
- ✅ Autenticación LDAP (Active Directory) con JWT
- ✅ Base de datos dual: SQL Server (ubicaciones) + MongoDB (specs hardware)
- ✅ Kubernetes en Minikube con Calico CNI
- ✅ NetworkPolicies Zero-Trust (default-deny-all + 4 allow rules)
- ✅ pfSense firewall perimetral con port forward WAN:443 → k8s:30443
- ✅ Acceso multi-red: WAN externo, LAN interna, VirtualBox port forwards
- ✅ CORS dinámico desde env vars (no hardcodeado)
- ✅ Todos los CRUD operacionales con control de permisos (EDITOR, ADMIN, VIEWER)
- ✅ Depliegue reproducible: manifiestos k8s + systemd service + documentación

## Equipo

| Integrante | Rol |
|------------|-----|
| Gonzalo | Implementación fullstack + infraestructura k8s + pfSense |

---

## Entregables

- [x] Esquema de arquitectura (servicios, puertos, reglas de red)
- [x] Modelo y scripts de bases de datos + JSON de documentos
- [x] Aplicación web con flujograma (React + Spring Boot)
- [x] Manifiestos Kubernetes y Network Policies
- [x] Ecosistema funcional en Minikube + pfSense
- [x] Documentación completa (README + informe técnico)
- [x] Repositorio Git con historial de evolución

---

## Documentación

| Recurso | Descripción |
|---------|-------------|
| [`README.md`](./README.md) | Este archivo: guía de inicio rápido, despliegue e integración |
| [`Proyecto Integrador EGI.md`](./Proyecto%20Integrador%20EGI.md) | Enunciado y requisitos del proyecto |
| [`docs/Informe_EGI_Inventario.md`](./docs/Informe_EGI_Inventario.md) | Informe técnico: arquitectura, modelo de datos, backend, Docker, seguridad, NetworkPolicies |
| [`firewall/reglas_pfsense.md`](./firewall/reglas_pfsense.md) | Diseño de red pfSense, interfaces, NAT, port forward, reglas Zero-Trust |
| [`.env.example`](./.env.example) | Plantilla de variables para `docker compose up` |
| `docs/` | Diagramas de topología, entidad-relación, flujo de aplicación |
| `k8s/` | Manifiestos Kubernetes: namespace, configmap, deployments, services, ingress, network policies |

---

## Licencia

<!-- TODO: Definir licencia si aplica -->

_Por definir._

---

<div align="center">

**ITU · EGI · 2026**

</div>
