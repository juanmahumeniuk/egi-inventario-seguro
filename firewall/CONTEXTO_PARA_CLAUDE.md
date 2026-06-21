# Contexto del Proyecto EGI — Para Claude Code

Este documento contiene TODO lo que necesitas saber para ayudar a levantar el ecosistema
de Kubernetes + pfSense del proyecto EGI Inventario Seguro.

---

## Que es este proyecto

Sistema web universitario (ITU) para inventariar equipos de laboratorio. Spring Boot (API REST)
con doble base de datos (SQL Server para ubicaciones + MongoDB para hardware), desplegado sobre
Kubernetes con politicas de red Zero-Trust y firewall perimetral pfSense.

**Repositorio:** https://github.com/juanmahumeniuk/egi-inventario-seguro
**Rama principal de trabajo:** `develop` (tiene todo mergeado: k8s + LDAP + frontend)

---

## Arquitectura de VMs (VirtualBox)

```
                Adaptador puente/NAT                    Red interna "egi-lan"
Internet/Host -------- WAN [pfSense] LAN --------+----------------+-----------------+
                       DHCP          192.168.1.1  |                |                 |
                                                  |                |                 |
                                             VM2 AD/DNS      VM3 SQL Server    VM4 Kubernetes
                                            192.168.1.10     192.168.1.20     192.168.1.40
```

| VM | Rol | IP | SO |
|----|-----|----|----|
| VM1 | pfSense (firewall/gateway) | WAN: DHCP, LAN: 192.168.1.1 | pfSense (FreeBSD) |
| VM2 | Active Directory + DNS | 192.168.1.10 | Windows Server |
| VM3 | SQL Server 2022 | 192.168.1.20 | Windows Server |
| VM4 | Kubernetes (minikube) | 192.168.1.40 | Ubuntu/Linux |

**Red LAN:** `192.168.1.0/24` — todas las VMs internas usan red interna "egi-lan" de VirtualBox.
**Gateway de todas:** `192.168.1.1` (pfSense LAN).
**DNS:** `192.168.1.10` (VM2 AD).

---

## Kubernetes — Lo que hay que levantar en VM4

### Prerrequisitos en VM4
- Docker instalado
- minikube instalado
- kubectl instalado

### Iniciar el cluster (OBLIGATORIO con Calico)
```bash
minikube start --cni=calico --driver=docker --memory=6144 --cpus=4
minikube addons enable ingress
```

> **CRITICO:** sin `--cni=calico` al crear el cluster, las NetworkPolicies NO funcionan.
> Agregarlo despues obliga a recrear el cluster.

### Construir y cargar la imagen de la app
```bash
cd <ruta-del-repo>/app/inventario-web
docker build -t inventario-web:latest .
minikube image load inventario-web:latest
```

### Crear los Secrets (NO estan en el repo, hay que crearlos a mano)

El deployment `inventario-web` referencia 4 secrets via `envFrom` que **no existen como manifiestos
en el repositorio**. Sin crearlos, el pod no arranca.

```bash
# sql-secret: contiene la password de SQL Server
kubectl -n inventario-seguro create secret generic sql-secret \
  --from-literal=DB_PASSWORD='Itu12345!'

# ldap-secret: credenciales de LDAP (si ya esta configurado el AD)
kubectl -n inventario-seguro create secret generic ldap-secret \
  --from-literal=LDAP_BIND_DN='cn=Administrador,cn=Users,dc=itu,dc=local' \
  --from-literal=LDAP_BIND_PASSWORD='Itu12345!'

# mongo-secret: MongoDB no usa auth en este setup, pero el deployment lo referencia
kubectl -n inventario-seguro create secret generic mongo-secret

# jwt-secret: clave de firma JWT
kubectl -n inventario-seguro create secret generic jwt-secret \
  --from-literal=JWT_SECRET='SecretKeyForEGIIinventarioSeguroNeedsToBeAtLeast32BytesLongSecretKeyForEGIIinventarioSeguroNeedsToBeAtLeast32BytesLong'
```

> **IMPORTANTE:** el namespace `inventario-seguro` debe existir antes de crear los secrets.
> Aplica `k8s/namespace.yaml` primero.

### Aplicar manifiestos (en orden)

```bash
# 1. Namespace
kubectl apply -f k8s/namespace.yaml

# 2. ConfigMap
kubectl apply -f k8s/configmap.yaml

# 3. Crear los 4 secrets (ver comandos arriba)

# 4. Storage + MongoDB
kubectl apply -f k8s/storage/mongodb-pvc.yaml
kubectl apply -f k8s/deployments/mongodb.yaml
kubectl apply -f k8s/services/mongodb.yaml

# 5. App (esperar a que mongo este Ready primero)
kubectl apply -f k8s/deployments/inventario-web.yaml
kubectl apply -f k8s/services/inventario-web.yaml

# 6. Ingress + NodePort para pfSense
kubectl apply -f k8s/ingress/inventario-ingress.yaml
kubectl apply -f k8s/ingress/ingress-nginx-nodeport.yaml

# 7. Verificar SIN policies primero (debe responder HTTP 200)
#    Desde dentro de la VM4:
minikube ssh -- "curl -sk -o /dev/null -w 'HTTP %{http_code}\n' \
  https://localhost:30443/api/maquinas -H 'Host: inventario.itu.local'"

# 8. Aplicar NetworkPolicies (TODAS JUNTAS, no de a una)
kubectl apply -f k8s/network-policies/

# 9. Verificar que sigue respondiendo (Zero-Trust activo + allow rules)
minikube ssh -- "curl -sk -o /dev/null -w 'HTTP %{http_code}\n' \
  https://localhost:30443/api/maquinas -H 'Host: inventario.itu.local'"
```

### Estructura de manifiestos k8s/ en el repo (rama develop)

```
k8s/
  namespace.yaml                         # Namespace: inventario-seguro
  configmap.yaml                         # ConfigMap: app-config (Mongo URI, DB_URL, CORS)
  storage/
    mongodb-pvc.yaml                     # PVC 5Gi para MongoDB
  deployments/
    mongodb.yaml                         # Deployment: mongodb (mongo:7)
    inventario-web.yaml                  # Deployment: inventario-web (imagen local)
  services/
    mongodb.yaml                         # Service ClusterIP :27017
    inventario-web.yaml                  # Service ClusterIP :8080
  ingress/
    inventario-ingress.yaml              # Ingress: host inventario.itu.local -> inventario-web:8080
    ingress-nginx-nodeport.yaml          # Service NodePort 30443 para pfSense
  network-policies/
    default-deny-all.yaml                # Zero-Trust: bloquea todo por defecto
    allow-ingress-to-web.yaml            # Permite: ingress-nginx -> inventario-web
    allow-web-to-mongodb.yaml            # Permite: inventario-web <-> mongodb (ingress + egress)
    allow-web-to-external-services.yaml  # Permite: inventario-web -> VMs externas (SQL, LDAP, DNS)
```

---

## Configuracion del backend (application.yml)

El backend Spring Boot necesita:

| Variable | Valor para el despliegue real | Donde se configura |
|----------|-------------------------------|-------------------|
| `DB_URL` | `jdbc:sqlserver://192.168.1.20:1433;databaseName=inventario_egi;...` | ConfigMap `app-config` |
| `DB_USER` | `sa` | ConfigMap `app-config` |
| `DB_PASSWORD` | `Itu12345!` | Secret `sql-secret` |
| `MONGO_URI` | `mongodb://mongodb:27017/inventario_egi` | ConfigMap `app-config` |
| LDAP | Hardcodeado en application.yml: `ldap://192.168.1.10:389` | application.yml |

**Flyway** corre automaticamente al arrancar el backend y crea las tablas en SQL Server (V1-V4).
La base de datos `inventario_egi` debe existir previamente en SQL Server.

Para crearla (en VM3 o via sqlcmd):
```sql
IF DB_ID('inventario_egi') IS NULL
    CREATE DATABASE inventario_egi;
GO
```

---

## pfSense — Port forward critico

El port forward que conecta todo:

```
Cliente externo -> pfSense WAN:443 -> NAT -> 192.168.1.40:30443 -> ingress-nginx -> inventario-web
```

Configuracion en pfSense GUI (**Firewall -> NAT -> Port Forward**):

| Campo | Valor |
|-------|-------|
| Interface | WAN |
| Protocol | TCP |
| Destination | WAN address |
| Destination port | HTTPS (443) |
| Redirect target IP | 192.168.1.40 |
| Redirect target port | 30443 |
| Filter rule association | Add associated filter rule |

**Gotcha:** hay que desmarcar "Block private networks" y "Block bogon networks" en
Interfaces -> WAN, o el port forward no funciona (la WAN esta en red privada de VirtualBox).

---

## NetworkPolicies (Calico) — Resumen

| Policy | Que hace |
|--------|----------|
| `default-deny-all` | Bloquea TODO ingress y egress en el namespace |
| `allow-ingress-to-web` | Permite: pods de ingress-nginx -> inventario-web |
| `allow-web-to-mongodb` | Permite: inventario-web <-> mongodb (puerto 27017) |
| `allow-web-to-mongodb-egress` | (mismo archivo) Egress de web a mongo |
| `allow-web-to-external-services` | Permite egress de web a: SQL (192.168.1.20:1433), LDAP (192.168.1.10:389/636), DNS (192.168.1.10:53), CoreDNS interno |

**CRITICO:** aplicar `default-deny-all` y las 4 allow rules **juntas** (un solo `kubectl apply -f k8s/network-policies/`).
Si aplicas solo el deny, todo se corta hasta que esten los allows.

---

## NodePort 30443 — Gotcha con driver Docker

Con `--driver=docker` (el default en minikube), el NodePort 30443 queda **dentro del contenedor
de minikube**, no directamente en la IP de la VM. Soluciones:

1. **`minikube tunnel`** (corre en foreground, expone todo)
2. **iptables manual** en la VM4: `sudo iptables -t nat -A PREROUTING -p tcp --dport 30443 -j DNAT --to-destination $(minikube ip):30443`
3. **`--driver=none`** (minikube corre directo en la VM, sin contenedor — el NodePort queda en la IP de la VM)

Para el setup con pfSense, la opcion 3 o un port-forward persistente es lo mas limpio.

---

## Seeds de MongoDB (opcional)

Una vez que MongoDB este corriendo, se pueden cargar datos de ejemplo:

```bash
# Desde la VM4, port-forward del service:
kubectl -n inventario-seguro port-forward svc/mongodb 27017:27017 &

# Ejecutar los scripts de seed (V2 actualiza el validador, V3 inserta 3 docs):
mongosh mongodb://localhost:27017/inventario_egi migraciones/mongodb/V2__update_maquina_schema.js
mongosh mongodb://localhost:27017/inventario_egi migraciones/mongodb/V3__seed_maquina_documents.js
```

---

## Problemas comunes

| Problema | Causa | Solucion |
|----------|-------|----------|
| Pod inventario-web en CrashLoopBackOff | SQL Server no alcanzable o DB no existe | Verificar `ping 192.168.1.20` desde el pod; crear la DB con sqlcmd |
| Pod no arranca (evento "secret not found") | Faltan los 4 secrets | Crearlos con `kubectl create secret` (ver arriba) |
| curl al NodePort 30443 no responde | Con driver Docker, el port queda interno | Usar `minikube tunnel` o `--driver=none` |
| NetworkPolicies no bloquean nada | Cluster sin Calico | Recrear cluster con `minikube start --cni=calico` |
| pfSense port forward no llega | "Block private networks" activo en WAN | Desmarcar en Interfaces -> WAN |
| DNS no resuelve desde VMs internas | DNS no apunta a VM2 | Configurar DNS=192.168.1.10 en cada VM |

---

## Verificacion de punta a punta

Cuando todo este levantado, este comando desde **fuera del cluster** (host fisico o red WAN)
debe devolver HTTP 200:

```bash
curl -k https://<IP_WAN_PFSENSE>/api/maquinas -H 'Host: inventario.itu.local'
```

El flujo completo que se esta probando:
```
curl -> pfSense WAN:443 -> NAT port forward -> VM4:30443
  -> ingress-nginx (cert autofirmado)
    -> Ingress (host: inventario.itu.local)
      -> Service inventario-web:8080
        -> Spring Boot API
          -> SQL Server (192.168.1.20) + MongoDB (in-cluster)
```
