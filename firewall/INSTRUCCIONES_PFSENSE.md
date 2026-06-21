# Instrucciones para Ignacio: Configurar pfSense desde cero

Guia paso a paso para levantar pfSense en VirtualBox y conectarlo al ecosistema k8s.

---

## Requisitos previos

- VirtualBox instalado
- ISO de pfSense descargada (Community Edition)
- Las VMs de AD/DNS (VM2), SQL Server (VM3) y Kubernetes (VM4) ya existen (o las vas a crear)

---

## Paso 1: Crear la VM de pfSense en VirtualBox

1. **Nueva VM**: Nombre `pfSense`, Tipo `BSD`, Version `FreeBSD (64-bit)`
2. **RAM**: 1024 MB (minimo 512)
3. **Disco**: 8 GB dinamico (pfSense usa muy poco)
4. **Red** (IMPORTANTE - con la VM apagada):
   - **Adaptador 1**: Habilitado -> `Adaptador puente` (o `NAT`) -> sera la **WAN**
   - **Adaptador 2**: Habilitado -> `Red interna`, nombre exacto: **`egi-lan`** -> sera la **LAN**

---

## Paso 2: Configurar las placas de red de las otras VMs

Con cada VM **apagada**:

| VM | Adaptador 1 |
|----|------------|
| VM2 (AD/DNS) | Red interna, nombre: **`egi-lan`** |
| VM3 (SQL Server) | Red interna, nombre: **`egi-lan`** |
| VM4 (Kubernetes) | Red interna, nombre: **`egi-lan`** |

> El nombre `egi-lan` debe ser **exactamente igual** en todas. VirtualBox lo trata como un switch virtual.

---

## Paso 3: Instalar pfSense

1. Montar la ISO y bootear la VM
2. Aceptar la instalacion por defecto (ZFS o UFS, da igual)
3. Al terminar, quitar la ISO y reiniciar

---

## Paso 4: Asignar interfaces (consola de pfSense)

Al primer arranque, pfSense pregunta por consola:

1. **Should VLANs be set up?** -> `n`
2. **Enter the WAN interface name** -> `em0` (o `vtnet0` si usa virtio)
3. **Enter the LAN interface name** -> `em1` (o `vtnet1`)
4. Confirmar

---

## Paso 5: Configurar IPs (consola, opcion 2)

Seleccionar opcion **2) Set interface(s) IP address**:

### WAN:
- Configurar via DHCP: **si**
- (VirtualBox le asigna IP automaticamente)

### LAN:
- IP: **`192.168.1.1`**
- Mascara: **`24`**
- Gateway: (dejar vacio, es la LAN)
- DHCP server en LAN: **si**
- Rango DHCP: **`192.168.1.100`** a **`192.168.1.200`**
- Revert to HTTP for webConfigurator: **si** (mas facil para empezar)

---

## Paso 6: Configurar las VMs internas con IP fija

Cada VM interna necesita IP estatica con gateway `192.168.1.1`:

| VM | IP | Gateway | DNS |
|----|----|---------|----|
| VM2 (AD/DNS) | `192.168.1.10` | `192.168.1.1` | `127.0.0.1` (es el DNS) |
| VM3 (SQL Server) | `192.168.1.20` | `192.168.1.1` | `192.168.1.10` |
| VM4 (Kubernetes) | `192.168.1.40` | `192.168.1.1` | `192.168.1.10` |

Para verificar conectividad desde cualquier VM interna:
```bash
ping 192.168.1.1      # gateway (pfSense LAN)
ping 8.8.8.8          # internet (via NAT de pfSense)
ping google.com       # DNS
```

---

## Paso 7: Acceder a la GUI de pfSense

Desde **cualquier VM de la LAN** (VM2, VM3 o VM4), abrir un navegador:

```
http://192.168.1.1
```

- Usuario: `admin`
- Password: `pfsense`
- **Cambiar la password en el wizard inicial**

---

## Paso 8: Quitar bloqueo de redes privadas en WAN

**Sin este paso, el port forward NO funciona.**

1. Ir a **Interfaces -> WAN**
2. Bajar hasta el final
3. **Desmarcar**:
   - `Block private networks and loopback addresses`
   - `Block bogon networks`
4. **Save** y **Apply Changes**

---

## Paso 9: Verificar Outbound NAT

1. Ir a **Firewall -> NAT -> pestaña Outbound**
2. Debe estar en **Automatic outbound NAT rule generation**
3. Verificar que aparezca una regla que enmascara `192.168.1.0/24` por la WAN
4. Si las VMs ya hacen ping a 8.8.8.8, esto ya funciona

---

## Paso 10: Crear el Port Forward (la regla estrella)

Esto es lo que conecta el mundo exterior con el Ingress de Kubernetes.

1. Ir a **Firewall -> NAT -> pestaña Port Forward**
2. Click en **Add** (flecha arriba)
3. Configurar:

| Campo | Valor |
|-------|-------|
| Interface | **WAN** |
| Protocol | **TCP** |
| Destination | **WAN address** |
| Destination port range | From: **HTTPS (443)** / To: **HTTPS (443)** |
| Redirect target IP | **`192.168.1.40`** |
| Redirect target port | **`30443`** |
| Description | `HTTPS externo -> Ingress K8s` |
| Filter rule association | **Add associated filter rule** |

4. **Save** y **Apply Changes**

---

## Paso 11: Verificar las reglas de firewall

1. Ir a **Firewall -> Rules -> WAN**
2. Debe aparecer la regla creada automaticamente por el port forward (TCP 443)
3. Todo lo demas entrante por WAN queda **denegado por defecto** (Zero-Trust perimetral)

En **Firewall -> Rules -> LAN**:
- Por defecto trae "allow LAN to any" (suficiente para arrancar)
- Para Zero-Trust fino, mas adelante acotar a:

| Origen | Destino | Puerto | Motivo |
|--------|---------|--------|--------|
| VM4 (192.168.1.40) | VM3 (192.168.1.20) | 1433/TCP | SQL Server |
| VM4 (192.168.1.40) | VM2 (192.168.1.10) | 389, 636/TCP | LDAP |
| VM4 (192.168.1.40) | VM2 (192.168.1.10) | 53 TCP/UDP | DNS |
| resto | cualquiera | - | DENEGAR |

---

## Paso 12: Verificacion final

### Desde VM4 (Kubernetes):
```bash
# Salida a internet
ping 8.8.8.8
ping google.com

# Conectividad a otras VMs
ping 192.168.1.10   # AD
ping 192.168.1.20   # SQL
```

### Desde fuera (host o red WAN):
```bash
# La IP WAN de pfSense la ves en Status -> Interfaces en la GUI
curl -k https://<IP_WAN_DE_PFSENSE>
# Debe llegar al Ingress de K8s (si el cluster esta levantado)
```

### En pfSense:
- **Diagnostics -> States**: ver conexiones activas
- **Status -> System Logs -> Firewall**: ver que pasa y que bloquea

---

## Orden recomendado de trabajo

1. Crear VM pfSense y configurar placas de red (pasos 1-2)
2. Instalar pfSense y asignar interfaces/IPs (pasos 3-5)
3. Configurar IPs fijas en VMs internas (paso 6)
4. Verificar conectividad basica: ping entre VMs y a internet (paso 6)
5. Acceder a la GUI y hacer el wizard (paso 7)
6. Desmarcar bloqueo de redes privadas (paso 8)
7. Configurar port forward WAN:443 -> VM4:30443 (paso 10)
8. Levantar minikube + k8s en VM4 (ver doc del otro Claude)
9. Probar el port forward de punta a punta (paso 12)
10. Aplicar NetworkPolicies y verificar Zero-Trust (paso 11 + k8s)

---

## Esquema de red completo

```
                Adaptador puente/NAT                    Red interna "egi-lan"
Internet/Host -------- WAN [pfSense] LAN --------+----------------+-----------------+
                       DHCP          192.168.1.1  |                |                 |
                                                  |                |                 |
                                             VM2 AD/DNS      VM3 SQL Server    VM4 Kubernetes
                                            192.168.1.10     192.168.1.20     192.168.1.40
                                                                               minikube + Calico
                                                                               NodePort 30443
                                                                                    |
                                                                              ingress-nginx
                                                                                    |
                                                                           inventario-web:8080
```

Flujo de una peticion externa:
```
Cliente -> pfSense WAN:443 -> NAT -> VM4:30443 -> ingress-nginx -> inventario-web:8080
```
