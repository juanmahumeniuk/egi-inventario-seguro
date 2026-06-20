# Informe de Creación de Imágenes (Rama `devops`)

Este informe detalla la estrategia de contenerización utilizada actualmente en la rama `devops` para los componentes principales del sistema `egi-inventario`. He analizado los archivos `Dockerfile` y la buena noticia es que **ambos implementan "Multi-stage builds"** (construcciones en múltiples etapas). 

Esta es una excelente práctica DevOps porque separa el entorno "pesado" (donde se compila el código y se bajan librerías) del entorno "ligero" (el que realmente corre en producción).

---

## 1. Imagen del Backend (Spring Boot / Java 17)
**Ubicación:** `app/inventario-web/Dockerfile`

### Etapa 1: Compilación (Build)
- **Imagen Base:** `maven:3.9-eclipse-temurin-17`
- **Proceso:** 
  1. Copia el archivo `pom.xml` y la carpeta de código fuente `src` dentro del contenedor.
  2. Ejecuta `mvn package -DskipTests` para descargar las dependencias y compilar el código Java en un archivo `.jar` ejecutable. Al omitir las pruebas (`skipTests`), el empaquetado es mucho más rápido.

### Etapa 2: Producción (Runtime)
- **Imagen Base:** `eclipse-temurin:17-jre-alpine` (La etiqueta Alpine indica que es una distribución de Linux ultra ligera orientada a la seguridad).
- **Proceso:**
  1. Extrae **únicamente** el archivo compilado (`inventario-seguro-*.jar`) generado en la etapa anterior. Todo el entorno de Maven y el código fuente se descartan.
  2. Expone el puerto `8080`.
  3. Define el comando de arranque para el contenedor (`java -jar app.jar`).
- **Beneficio DevOps:** La imagen final pesa una fracción del tamaño original, lo cual reduce la "superficie de ataque" para vulnerabilidades y acelera enormemente el tiempo de subida a los registros o despliegue en Kubernetes.

---

## 2. Imagen del Frontend (React/Vite)
**Ubicación:** `app/inventario-web/frontend/Dockerfile`

### Etapa 1: Compilación (Build)
- **Imagen Base:** `node:20-alpine`
- **Proceso:**
  1. Instala las dependencias de Node.js mediante `npm ci`, garantizando versiones exactas según el `package-lock.json`.
  2. Acepta y configura variables como `VITE_API_URL` (mediante argumentos de compilación `ARG` y `ENV`). Es en este momento donde la URL del backend se "quema" en el código estático de la aplicación.
  3. Ejecuta `npm run build` para minificar y agrupar todo en archivos HTML, JS y CSS en la carpeta `dist/`.

### Etapa 2: Producción (Runtime)
- **Imagen Base:** `nginx:alpine`
- **Proceso:**
  1. Configura el servidor web sobrescribiendo el archivo por defecto con un `nginx.conf` local (vital para aplicaciones de una sola página "SPA", asegurando que las URLs internas no den error 404).
  2. Copia los archivos minificados generados previamente hacia la ruta pública de NGINX `/usr/share/nginx/html`.
  3. Expone el puerto `80`.
- **Beneficio DevOps:** No se requiere instalar ni correr Node.js en producción. Servir archivos puramente estáticos usando NGINX consume poquísima memoria RAM y soporta mucho más tráfico concurrente.

---

## 3. Recomendaciones y Conclusiones para la Rama

1. **Variables de Inyección en el Build (Frontend):** Al ser una SPA de Vite, la variable de entorno `VITE_API_URL` debe conocerse en el momento de crear la imagen. Al construirla para el entorno de Minikube que hemos diseñado antes, el comando ideal sería:
   ```bash
   minikube image build \
     --build-arg VITE_API_URL=https://egi-inventario.local:30443/api \
     -t egi-frontend:latest \
     ./app/inventario-web/frontend
   ```

2. **Caché de Capas:** Ambos Dockerfiles están muy bien optimizados en cuanto al copiado del gestor de dependencias (`pom.xml` y `package.json`) *antes* de copiar el resto del código (`src`). Esto permite aprovechar el caché de Docker y evitar volver a descargar internet entero si solo modificaste un archivo de código.
