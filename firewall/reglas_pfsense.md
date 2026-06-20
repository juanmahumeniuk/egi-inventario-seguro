# Firewall Perimetral — pfSense (VM 1)

Configuración del firewall perimetral del ecosistema EGI. La **VM 1** corre **pfSense**
y actúa como router/gateway de todo el laboratorio y como único punto de entrada
(simulación de DMZ institucional) hacia el clúster Kubernetes de la VM 4.

> **Decisión de equipo:** se eligió **pfSense** sobre UFW/GUFW porque el rol de la VM 1
> es un firewall *perimetral* (gateway entre redes con NAT), no un firewall de host.
> pfSense modela NAT, reglas por interfaz y DMZ de forma nativa, coherente con la
> topología del informe.

---

## 1. Diseño de red

pfSense tiene **2 placas de red** y enruta entre la red externa (WAN) y la red interna
del laboratorio (LAN), donde viven el resto de las VMs.

```
                Adaptador 1 (Puente/NAT)              Adaptador 2 (Red interna "egi-lan")
 Internet/Host ───────── WAN  [ pfSense ]  LAN ───────────┬──────────────┬──────────────┐
                                                          │              │              │
                                                    VM2 AD/DNS      VM3 SQL        VM4 K8s
                                                   192.168.10.10  192.168.10.20  192.168.10.40
```

### Esquema de IPs

| Elemento                 | Interfaz / IP            |
|--------------------------|--------------------------|
| pfSense WAN              | DHCP (la da VirtualBox)  |
| pfSense LAN (gateway)    | `192.168.10.1/24`        |
| VM2 — AD / DNS           | `192.168.10.10`          |
| VM3 — SQL Server         | `192.168.10.20`          |
| VM4 — Kubernetes         | `192.168.10.40`          |
| Rango DHCP de la LAN     | `192.168.10.100–.200`    |
| Puerto Ingress (NodePort)| `30443`                  |

---

## 2. Configuración de placas en VirtualBox

Con todas las VMs **apagadas**:

**pfSense** → Configuración → Red:
- **Adaptador 1**: Habilitado → `Adaptador puente` (recomendado) o `NAT` → será la **WAN**.
- **Adaptador 2**: Habilitado → `Red interna`, nombre **`egi-lan`** → será la **LAN**.

**VM2 (AD), VM3 (SQL), VM4 (K8s)** → cada una:
- **Adaptador 1**: Habilitado → `Red interna`, nombre **`egi-lan`** (mismo nombre exacto).

> La VM4 (minikube) necesita internet para bajar imágenes; por eso la WAN debe ser
> `Puente` o `NAT` (no `Solo-anfitrión`). El outbound NAT de pfSense (sección 5) le da
> salida a internet a todas las VMs internas.

---

## 3. Asignación de interfaces e IPs en pfSense (consola)

Al primer arranque de pfSense por consola:

1. ¿VLANs? → `n`.
2. **Assign interfaces**: normalmente `em0` = WAN, `em1` = LAN (Adaptador 1 y 2). Confirmar.
3. Opción **2) Set interface(s) IP address**:
   - **WAN** → DHCP.
   - **LAN** → IP estática `192.168.10.1`, máscara `/24`.
     Activar el **servidor DHCP** de la LAN con rango `192.168.10.100`–`192.168.10.200`.
4. VMs internas: DHCP, o IP estática de la tabla con gateway `192.168.10.1` y
   DNS → `192.168.10.10` (VM2).

A partir de acá todo se hace desde la **GUI web**: entrar desde una VM de la LAN a
`https://192.168.10.1` (usuario `admin` / contraseña `pfsense` la primera vez; cambiarla).

---

## 4. Quitar el bloqueo de redes privadas en la WAN ⚠️

Como la WAN está en una red privada (NAT/puente de laboratorio), pfSense por defecto
descarta ese tráfico. **Sin este paso, el port forward no funciona.**

- **Interfaces → WAN** → al final → **desmarcar**:
  - [ ] *Block private networks and loopback addresses*
  - [ ] *Block bogon networks*
- Guardar y aplicar.

---

## 5. Outbound NAT (salida de las VMs a internet)

- **Firewall → NAT → pestaña Outbound**.
- Modo: **Automatic outbound NAT** (por defecto).
- Verificar la regla automática que enmascara `192.168.10.0/24` saliendo por WAN.

Con esto las VMs internas salen a internet a través de pfSense.

---

## 6. Port Forward: HTTPS externo → Ingress de Kubernetes ⭐

Materializa la flecha `Firewall → Ingress` del informe.

**Firewall → NAT → pestaña Port Forward → Add:**

| Campo                   | Valor                              |
|-------------------------|------------------------------------|
| Interface               | **WAN**                            |
| Protocol                | **TCP**                            |
| Destination             | **WAN address**                    |
| Destination port range  | **HTTPS (443)**                    |
| Redirect target IP      | `192.168.10.40` (VM4 Kubernetes)   |
| Redirect target port    | `30443` (NodePort del Ingress)     |
| Description             | `HTTPS externo -> Ingress K8s`     |
| Filter rule association | **Add associated filter rule**     |

Guardar y aplicar. La opción *Add associated filter rule* crea automáticamente la regla
de firewall en WAN que permite este tráfico.

> **Dependencia con Kubernetes:** el Ingress debe quedar accesible en la IP LAN de la VM4
> (`192.168.10.40`) en el puerto `30443`. Según el driver de minikube, el NodePort puede
> quedar dentro de minikube y no en la IP de la VM; eso se resuelve al armar el clúster
> (minikube tunnel / `--driver` adecuado / reenvío dentro de la VM).

---

## 7. Reglas de firewall (Zero-Trust / default-deny)

1. **Firewall → Rules → WAN**: existe ya la regla creada por el port forward (TCP 443).
   Todo lo demás entrante por WAN queda **denegado por defecto** → default-deny perimetral.
2. **Firewall → Rules → LAN**: por defecto trae "allow LAN to any". Para Zero-Trust fino
   se acota más adelante a lo estrictamente necesario entre VMs:

| Origen (LAN)   | Destino        | Puerto        | Motivo                         |
|----------------|----------------|---------------|--------------------------------|
| VM4 K8s        | VM3 SQL Server | 1433/TCP      | Consulta de ubicaciones        |
| VM4 K8s        | VM2 AD/LDAP    | 389, 636/TCP  | Autenticación LDAP/LDAPS       |
| VM4 K8s        | VM2 DNS        | 53 TCP/UDP    | Resolución de nombres interna  |
| (resto)        | (cualquiera)   | —             | Denegado                       |

> El acotado fino de la LAN es un refinamiento posterior; para el primer arranque alcanza
> con la regla por defecto.

---

## 8. Verificación

1. **Outbound**: desde VM4 → `ping 8.8.8.8` y `ping google.com` (valida NAT + DNS).
2. **Estados**: pfSense → **Diagnostics → States** muestra las conexiones activas.
3. **Port forward**: desde host/cliente externo → `curl -k https://<IP_WAN_pfSense>`
   debe llegar al Ingress (con K8s levantado).
4. **Logs**: **Status → System Logs → Firewall** para ver qué pasa/bloquea.

---

## Pendientes / decisiones tomadas

- [x] Puerto del Ingress: **NodePort 30443**.
- [x] Esquema de IPs: red `192.168.10.0/24`.
- [x] WAN: `Adaptador puente` (fallback: `NAT` con reenvío en VirtualBox).
- [ ] Instalar las VMs y aplicar esta guía.
- [ ] Acotar reglas finas de la LAN (sección 7) una vez levantado el clúster.
- [ ] Actualizar el informe: hoy dice "GUFW/UFW (o pfSense como alternativa)";
      debe reflejar pfSense como opción elegida y referenciar `firewall/reglas_pfsense.md`.
