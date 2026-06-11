# Proyecto Integrador (EGI): Ecosistema de Inventario Seguro

## 📋 Contexto del Problema

<span style="color:orange">
La universidad (ITU) necesita un sistema centralizado para inventariar las computadoras de los laboratorios de informática. El equipo de desarrollo creará una aplicación web considerando que los datos del inventario se guarden en dos bases de datos, una SQL Server y otra MongoDB teniendo en cuenta que el área de infraestructura exige que el despliegue sea altamente seguro; por ende, debe cumplir con las políticas de red perimetrales del Firewall de la universidad (usando GUFW o pfSense) y teniendo en cuenta que tanto los usuarios de las bases de datos como de la aplicación se autentican contra el servidor Active Directory/LDAP institucional.

La aplicación web debería ser capaz de consultar SQL Server para saber dónde está la máquina (aulas, laboratorios y números de banco/mesa, fecha de mantenimiento) y a quién le pertenece (responsable técnico, alumnos o docentes tienen equipos asignados temporalmente), y luego usar el ID para traer de MongoDB qué componentes tiene instalados internamente (fabricante, modelo, CPU, RAM, Disco, tipo [desktop/laptop], Sistema Operativo, monitor, mouse, teclado).

El proyecto se debe presentar de forma versionada en un repositorio de Git, en donde se pueda ver la evolución tanto del código de la aplicación y de cada microservicio, en donde TODOS correrán de forma contenerizada, así como el correspondiente armado de los manifiestos de kubernetes para soportar esos microservicios contenerizados, incluyendo las políticas de red necesarias (que permitirán el tráfico de red desde la red pública hacia este ecosistema e incluso en la red privada), teniendo en cuenta de cumplir siempre con el principio de menor privilegio en la red.

El proyecto debe ser resuelto por un equipo de 5 integrantes, la idea es que se repartan las tareas entre todos, y que luego ensamblen lo hecho por cada uno a través de un repositorio unificado.  El proyecto lo van a defender los 5 integrantes, cada uno va a exponer y defender una parte del proyecto.   El mismo debe incluir desde un esquema de la arquitectura de servicios (que describa el nombre del servicio y puertos de acceso), con las correspondientes reglas de red, un esquema de la estructura de la base de datos (modelización - análisis y diseño previo a la creación de la base de datos, archivo con el código para crear las bases de datos y los JSON con los documentos), así como el desarrollo de la aplicación web de inventario (debe incluir el flujograma de la misma) y obviamente una presentación en un formato compatible a Powerpoint o similar.

La defensa del proyecto se realizará 2 semanas antes de la finalización del semestre actual y tendrá una duración de 17 minutos por equipo, esto con la finalidad de tener tiempo para evaluar, ya que van a ser 7 grupos por comisión.
</span>

---

## 🔽 Arquitectura del Sistema
El ecosistema consta de:
1. **Frontend (`inventario-web`)**: Aplicación web que expone la interfaz gráfica.
2. **Base de Datos MongoDB (`inventario-db`)**: Instancia de MongoDB que almacena los registros de hardware (qué componentes tiene instalados internamente).  
3. **Base de Datos SQL Server (`ubicacion-db`)**: Instancia de SQL Server o MySQL que almacena los registros de ubicación (dónde está la máquina, a quién le pertenece, fechas de mantenimiento).
4. **Servidor de Identidad (`ldap-service`)**: Servidor de Active Directory/LDAP que centraliza los usuarios (profesores y administradores).
5. **Simulación Perimetral (GUFW o pfSense)**: El clúster debe emular que el tráfico entrante pasa obligatoriamente por una IP autorizada de la DMZ del Firewall.  Se recomienda el uso de GUFW (que es la interfaz gráfica del firewall de Linux llamado Uncomplicated FireWall).

## ⚙️ Desafíos Técnicos a Resolver (Objetivos del Proyecto)
1. **Resolver la Conectividad Interna**: Armar las configuraciones de puertos, variables de entorno y selectores del Deployment y Service de SQL Server/MySQL.
2. **Configurar el Acceso Externo Seguro**: Configurar el servicio del Frontend para que sea accesible desde la IP asignada por Minikube, simulando el NAT del pfSense.
3. **Implementar Zero-Trust con Network Policies**:

   - El Frontend debe hablar con SQL Server/MongoDB y Active Directory/LDAP, y ser accesible vía HTTP.
   - Las Bases de Datos **sólo** deben aceptar conexiones del Frontend.
   - En la Base de Datos **MongoDB** se debe crear una colección, insertar documentos con estructuras variadas, realizar búsquedas filtradas, actualizar registros y eliminar datos específicos.
   - La Base de Datos **MongoDB** debe ser accesible desde líneas de comando (shell del contenedor) para ejecutar queries manualmente.
   - El Servidor Active Directory/LDAP debe recibir tráfico de autenticación del Frontend.
   - Bloquear cualquier otro tráfico no autorizado dentro del Namespace.

## 📎 Restricciones del Laboratorio
- Estás trabajando en Minikube. Para que las `NetworkPolicies` tengan efecto, tu clúster debe haberse iniciado con un CNI compatible (ej. `minikube start --cni=calico`).

---
## ✅ Entregables

1. Esquemas
   - Documentación del proyecto
   - Esquema de la arquitectura de servicios (que describa el nombre del servicio, puertos de acceso y reglas de red)
   - Esquema de la estructura de la base de datos (análisis y diseño previo a la creación de la base de datos)
   - Diagramas de flujo de la aplicación
2. Repositorio de Git con todo el código usado.
   - Archivos con el código para crear las bases de datos y los JSON con los documentos)
   - Archivos correspondientes al desarrollo de la aplicación web de inventario
   - Manifiestos de kubernetes usados en el despliegue del ecosistema
3. Ecosistema funcional en Minikube.
4. Aplicación web con la funcionalidad de gestionar el inventario de las aulas.
5. Presentación en un formato compatible a Powerpoint o similar
